package io.lumify.storm.video;

import com.google.inject.Inject;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import io.lumify.core.model.properties.MediaLumifyProperties;
import io.lumify.core.model.properties.RawLumifyProperties;
import io.lumify.core.util.ProcessRunner;
import org.securegraph.Element;
import org.securegraph.Property;
import org.securegraph.Vertex;
import org.securegraph.mutation.ExistingElementMutation;
import org.securegraph.property.StreamingPropertyValue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class VideoMp4EncodingWorker extends GraphPropertyWorker {
    private static final String PROPERTY_KEY = VideoMp4EncodingWorker.class.getName();
    private ProcessRunner processRunner;

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        File mp4File = File.createTempFile("encode_mp4_", ".mp4");
        File mp4ReloactedFile = File.createTempFile("relocated_mp4_", ".mp4");
        try {
            processRunner.execute(
                    "ffmpeg",
                    new String[]{
                            "-y", // overwrite output files
                            "-i", data.getLocalFile().getAbsolutePath(),
                            "-vcodec", "libx264",
                            "-vprofile", "high",
                            "-preset", "slow",
                            "-b:v", "500k",
                            "-maxrate", "500k",
                            "-bufsize", "1000k",
                            "-vf", "scale=720:480",
                            "-threads", "0",
                            "-acodec", "libfdk_aac",
                            "-b:a", "128k",
                            "-f", "mp4",
                            mp4File.getAbsolutePath()
                    },
                    null,
                    data.getLocalFile().getAbsolutePath() + ": "
            );

            processRunner.execute(
                    "qt-faststart",
                    new String[]{
                            mp4File.getAbsolutePath(),
                            mp4ReloactedFile.getAbsolutePath()
                    },
                    null,
                    data.getLocalFile().getAbsolutePath() + ": "
            );

            ExistingElementMutation<Vertex> m = data.getElement().prepareMutation();

            InputStream mp4RelocatedFileIn = new FileInputStream(mp4ReloactedFile);
            try {
                StreamingPropertyValue spv = new StreamingPropertyValue(mp4RelocatedFileIn, byte[].class);
                spv.searchIndex(false);
                Map<String, Object> metadata = new HashMap<String, Object>();
                metadata.put(RawLumifyProperties.MIME_TYPE.getKey(), MediaLumifyProperties.MIME_TYPE_VIDEO_MP4);
                MediaLumifyProperties.VIDEO_MP4.addPropertyValue(m, PROPERTY_KEY, spv, metadata, data.getProperty().getVisibility());
                m.save(getAuthorizations());
            } finally {
                mp4RelocatedFileIn.close();
            }
        } finally {
            mp4File.delete();
            mp4ReloactedFile.delete();
        }
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        if (!property.getName().equals(RawLumifyProperties.RAW.getKey())) {
            return false;
        }
        String mimeType = (String) property.getMetadata().get(RawLumifyProperties.MIME_TYPE.getKey());
        if (mimeType == null || !mimeType.startsWith("video")) {
            return false;
        }

        if (MediaLumifyProperties.VIDEO_MP4.hasProperty(element, PROPERTY_KEY)) {
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
