package io.lumify.mapping;

import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.web.clientapi.model.util.ClientApiConverter;
import java.io.InputStream;
import org.securegraph.Element;
import org.securegraph.Metadata;
import org.securegraph.Property;
import org.securegraph.Vertex;
import org.securegraph.property.StreamingPropertyValue;

public abstract class AbstractDocumentMappingGraphPropertyWorker<T extends DocumentMapping> extends GraphPropertyWorker {
    private final Class<T> mappingClass;
    private final String multiValueKey;
    private String hasEntityIri;
    private String conceptTypeIri;

    protected AbstractDocumentMappingGraphPropertyWorker(final Class<T> mapClass, final String valKey) {
        this.mappingClass = mapClass;
        this.multiValueKey = valKey;
    }

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);
        hasEntityIri = getOntologyRepository().getRequiredRelationshipIRIByIntent("artifactHasEntity");
        conceptTypeIri = getOntologyRepository().getConceptIRIByIntent(getConceptIriIntent());
    }

    public String getHasEntityIri() {
        return hasEntityIri;
    }

    public String getConceptTypeIri() {
        return conceptTypeIri;
    }

    /**
     * @return the intent used to identify the concept type IRI
     */
    protected abstract String getConceptIriIntent();

    /**
     * @return the vertex ID prefix
     */
    protected abstract String getVertexIdPrefix();

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        if (conceptTypeIri != null) {
            Metadata metadata = data.createPropertyMetadata();
            LumifyProperties.CONCEPT_TYPE.setProperty(data.getElement(), conceptTypeIri, metadata,
                    data.getVisibility(), getAuthorizations());
        }

        StreamingPropertyValue mappingJson = LumifyProperties.MAPPING_JSON.getPropertyValue(data.getElement());
        DocumentMapping mapping = ClientApiConverter.toClientApi(mappingJson.readToString(), DocumentMapping.class);
        StreamingPropertyValue raw = LumifyProperties.RAW.getPropertyValue(data.getElement());
        try (InputStream rawIn = raw.getInputStream()) {
            MappingState state = new GPWMappingState(this, data, getGraph(), getAuthorizations(), getClass().getName());
            mapping.mapDocument(rawIn, state, getVertexIdPrefix());
        }
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        return property != null && LumifyProperties.MAPPING_JSON.getPropertyName().equals(property.getName());
    }

    /**
     * Exposing publicly.
     * @param data the work data
     * @param vertex the vertex to add
     */
    @Override
    public void addVertexToWorkspaceIfNeeded(GraphPropertyWorkData data, Vertex vertex) {
        super.addVertexToWorkspaceIfNeeded(data, vertex);
    }
}
