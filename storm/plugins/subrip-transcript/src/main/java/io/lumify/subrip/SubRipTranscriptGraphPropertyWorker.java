package io.lumify.subrip;

import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import io.lumify.core.ingest.video.VideoTranscript;
import io.lumify.core.model.audit.AuditAction;
import io.lumify.core.model.audit.AuditBuilder;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.storm.video.SubRip;
import org.securegraph.Element;
import org.securegraph.Property;
import org.securegraph.Vertex;
import org.securegraph.mutation.ExistingElementMutation;
import org.securegraph.property.StreamingPropertyValue;

import java.io.InputStream;
import java.util.Map;

public class SubRipTranscriptGraphPropertyWorker extends GraphPropertyWorker {
    private static final String PROPERTY_KEY = SubRipTranscriptGraphPropertyWorker.class.getName();

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        StreamingPropertyValue youtubeccValue = SubRipTranscriptFileImportSupportingFileHandler.SUBRIP_CC.getPropertyValue(data.getElement());
        VideoTranscript videoTranscript = SubRip.read(youtubeccValue.getInputStream());

        ExistingElementMutation<Vertex> m = data.getElement().prepareMutation();
        Map<String, Object> metadata = data.createPropertyMetadata();
        metadata.put(LumifyProperties.META_DATA_TEXT_DESCRIPTION, "Sub-rip Transcript");
        addVideoTranscriptAsTextPropertiesToMutation(m, PROPERTY_KEY, videoTranscript, metadata, data.getVisibility());
        Vertex v = m.save(getAuthorizations());
        // Auditing the new properties set and that this class analyzed the vertex
        new AuditBuilder()
                .auditAction(AuditAction.UPDATE)
                .user(getUser())
                .analyzedBy(getClass().getSimpleName())
                .vertexToAudit(v)
                .existingElementMutation(m)
                .auditExisitingVertexProperties(getAuthorizations())
                .auditAction(AuditAction.ANALYZED_BY)
                .auditVertex(getAuthorizations(), false);

        getGraph().flush();
        pushVideoTranscriptTextPropertiesOnWorkQueue(data.getElement(), PROPERTY_KEY, videoTranscript);
    }


    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        StreamingPropertyValue subripValue = SubRipTranscriptFileImportSupportingFileHandler.SUBRIP_CC.getPropertyValue(element);
        if (subripValue == null) {
            return false;
        }

        if (!property.getName().equals(LumifyProperties.RAW.getPropertyName())) {
            return false;
        }
        String mimeType = (String) property.getMetadata().get(LumifyProperties.MIME_TYPE.getPropertyName());
        if (mimeType == null || !mimeType.startsWith("video")) {
            return false;
        }

        return true;
    }
}
