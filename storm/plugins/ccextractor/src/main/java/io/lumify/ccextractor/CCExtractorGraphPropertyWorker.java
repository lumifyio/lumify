package io.lumify.ccextractor;

import com.google.inject.Inject;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import io.lumify.core.ingest.video.VideoTranscript;
import io.lumify.core.model.audit.AuditAction;
import io.lumify.core.model.audit.AuditBuilder;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.util.ProcessRunner;
import io.lumify.storm.video.SubRip;
import org.securegraph.Element;
import org.securegraph.Property;
import org.securegraph.Vertex;
import org.securegraph.mutation.ExistingElementMutation;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

public class CCExtractorGraphPropertyWorker extends GraphPropertyWorker {
    private static final String PROPERTY_KEY = CCExtractorGraphPropertyWorker.class.getName();
    private ProcessRunner processRunner;

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        File ccFile = File.createTempFile("ccextract", "txt");
        ccFile.delete();
        try {
            processRunner.execute(
                    "ccextractor",
                    new String[]{
                            "-o", ccFile.getAbsolutePath(),
                            "-in=mp4",
                            data.getLocalFile().getAbsolutePath()
                    },
                    null,
                    data.getLocalFile().getAbsolutePath() + ": "
            );

            VideoTranscript videoTranscript = SubRip.read(ccFile);
            if (videoTranscript.getEntries().size() == 0) {
                return;
            }

            ExistingElementMutation<Vertex> m = data.getElement().prepareMutation();
            Map<String, Object> metadata = data.createPropertyMetadata();
            metadata.put(LumifyProperties.META_DATA_TEXT_DESCRIPTION, "Close Caption");
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
        } finally {
            ccFile.delete();
        }
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
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

    @Override
    public boolean isLocalFileRequired() {
        return true;
    }

    @Inject
    public void setProcessRunner(ProcessRunner ffmpeg) {
        this.processRunner = ffmpeg;
    }
}
