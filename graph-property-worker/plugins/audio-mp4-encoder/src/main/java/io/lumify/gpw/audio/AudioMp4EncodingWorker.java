package io.lumify.gpw.audio;

import com.google.inject.Inject;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.properties.MediaLumifyProperties;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.core.util.ProcessRunner;
import org.securegraph.Element;
import org.securegraph.Metadata;
import org.securegraph.Property;
import org.securegraph.Vertex;
import org.securegraph.mutation.ExistingElementMutation;
import org.securegraph.property.StreamingPropertyValue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class AudioMp4EncodingWorker extends GraphPropertyWorker {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(AudioMp4EncodingWorker.class);
    private static final String PROPERTY_KEY = AudioMp4EncodingWorker.class.getName();
    private ProcessRunner processRunner;

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        File mp4File = File.createTempFile("encode_mp4_", ".mp4");
        try {
            processRunner.execute(
                    "ffmpeg",
                    new String[]{
                            "-y", // overwrite output files
                            "-i", data.getLocalFile().getAbsolutePath(),
                            "-acodec", "libfdk_aac",
                            mp4File.getAbsolutePath()
                    },
                    null,
                    data.getLocalFile().getAbsolutePath() + ": "
            );

            ExistingElementMutation<Vertex> m = data.getElement().prepareMutation();

            try (InputStream mp4FileIn = new FileInputStream(mp4File)) {
                StreamingPropertyValue spv = new StreamingPropertyValue(mp4FileIn, byte[].class);
                spv.searchIndex(false);
                Metadata metadata = new Metadata();
                metadata.add(LumifyProperties.MIME_TYPE.getPropertyName(), MediaLumifyProperties.MIME_TYPE_AUDIO_MP4, getVisibilityTranslator().getDefaultVisibility());
                MediaLumifyProperties.AUDIO_MP4.addPropertyValue(m, PROPERTY_KEY, spv, metadata, data.getProperty().getVisibility());
                m.save(getAuthorizations());
            }
        } finally {
            if (!mp4File.delete()) {
                LOGGER.warn("Could not delete file %s", mp4File.getAbsolutePath());
            }
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
        String mimeType = LumifyProperties.MIME_TYPE.getMetadataValue(property.getMetadata(), null);
        if (mimeType == null || !mimeType.startsWith("audio")) {
            return false;
        }

        if (MediaLumifyProperties.AUDIO_MP4.hasProperty(element, PROPERTY_KEY)) {
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
