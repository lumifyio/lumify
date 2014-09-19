package io.lumify.youtube;

import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import io.lumify.core.ingest.video.VideoTranscript;
import io.lumify.core.model.audit.AuditAction;
import io.lumify.core.model.audit.AuditBuilder;
import io.lumify.core.model.properties.LumifyProperties;
import org.securegraph.Element;
import org.securegraph.Property;
import org.securegraph.Vertex;
import org.securegraph.mutation.ExistingElementMutation;
import org.securegraph.property.StreamingPropertyValue;

import java.io.InputStream;

public class YoutubeTranscriptGraphPropertyWorker extends GraphPropertyWorker {
    private static final String PROPERTY_KEY = YoutubeTranscriptGraphPropertyWorker.class.getName();

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        StreamingPropertyValue youtubeccValue = YoutubeTranscriptFileImportSupportingFileHandler.YOUTUBE_CC.getPropertyValue(data.getElement());
        VideoTranscript videoTranscript = YoutubeccReader.read(youtubeccValue.getInputStream());

        ExistingElementMutation<Vertex> m = data.getElement().prepareMutation();
        addVideoTranscriptAsTextPropertiesToMutation(m, PROPERTY_KEY, videoTranscript, data.createPropertyMetadata(), data.getVisibility());
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

        StreamingPropertyValue youtubeccValue = YoutubeTranscriptFileImportSupportingFileHandler.YOUTUBE_CC.getPropertyValue(element);
        if (youtubeccValue == null) {
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
