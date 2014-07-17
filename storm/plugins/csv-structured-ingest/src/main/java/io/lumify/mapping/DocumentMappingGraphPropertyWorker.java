package io.lumify.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import io.lumify.core.ingest.term.extraction.TermExtractionResult;
import io.lumify.core.model.audit.AuditAction;
import io.lumify.core.model.properties.LumifyProperties;
import org.apache.commons.io.IOUtils;
import org.securegraph.Element;
import org.securegraph.Property;
import org.securegraph.Vertex;
import org.securegraph.mutation.ExistingElementMutation;
import org.securegraph.property.StreamingPropertyValue;

import java.io.*;
import java.util.Map;

public class DocumentMappingGraphPropertyWorker extends GraphPropertyWorker {
    private static final String MULTIVALUE_KEY = DocumentMappingGraphPropertyWorker.class.getName();
    private ObjectMapper jsonMapper;

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        StreamingPropertyValue mappingJson = LumifyProperties.MAPPING_JSON.getPropertyValue(data.getElement());
        String mappingJsonString = IOUtils.toString(mappingJson.getInputStream());
        DocumentMapping mapping = jsonMapper.readValue(mappingJsonString, DocumentMapping.class);

        String text = executeTextExtraction(in, data, mapping);
        executeTermExtraction(new ByteArrayInputStream(text.getBytes()), data, mapping);
    }

    private String executeTextExtraction(InputStream in, GraphPropertyWorkData data, DocumentMapping mapping) throws IOException {
        StringWriter writer = new StringWriter();
        mapping.ingestDocument(in, writer);
        String text = writer.toString();

        ExistingElementMutation<Vertex> m = data.getElement().prepareMutation();
        StreamingPropertyValue textValue = new StreamingPropertyValue(new ByteArrayInputStream(text.getBytes()), String.class);
        Map<String, Object> textMetadata = data.createPropertyMetadata();
        textMetadata.put(LumifyProperties.MIME_TYPE.getPropertyName(), "text/plain");
        textMetadata.put(LumifyProperties.META_DATA_TEXT_DESCRIPTION, "Text");
        LumifyProperties.TEXT.addPropertyValue(m, MULTIVALUE_KEY, textValue, textMetadata, data.getVisibility());
        LumifyProperties.TITLE.addPropertyValue(m, MULTIVALUE_KEY, mapping.getSubject(), data.createPropertyMetadata(), data.getVisibility());
        Vertex v = m.save(getAuthorizations());
        getAuditRepository().auditVertexElementMutation(AuditAction.UPDATE, m, v, MULTIVALUE_KEY, getUser(), data.getVisibility());
        getAuditRepository().auditAnalyzedBy(AuditAction.ANALYZED_BY, v, getClass().getSimpleName(), getUser(), v.getVisibility());

        getGraph().flush();

        getWorkQueueRepository().pushGraphPropertyQueue(data.getElement(), MULTIVALUE_KEY, LumifyProperties.TITLE.getPropertyName());
        return text;
    }

    private void executeTermExtraction(InputStream in, GraphPropertyWorkData data, DocumentMapping mapping) throws IOException {
        TermExtractionResult termExtractionResult = mapping.mapDocument(new InputStreamReader(in), getClass().getName(), data.getProperty().getKey(), data.getVisibility());
        saveTermExtractionResult((Vertex) data.getElement(), termExtractionResult);
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        StreamingPropertyValue mappingJson = LumifyProperties.MAPPING_JSON.getPropertyValue(element);
        if (mappingJson == null) {
            return false;
        }

        String mimeType = (String) property.getMetadata().get(LumifyProperties.MIME_TYPE.getPropertyName());
        return !(mimeType == null || !mimeType.startsWith("text"));
    }

    @Inject
    public void setJsonMapper(final ObjectMapper mapper) {
        this.jsonMapper = mapper;
    }
}
