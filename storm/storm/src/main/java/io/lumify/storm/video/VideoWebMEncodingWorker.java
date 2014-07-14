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

public class VideoWebMEncodingWorker extends GraphPropertyWorker {
    private static final String PROPERTY_KEY = VideoWebMEncodingWorker.class.getName();
    private ProcessRunner processRunner;

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {

        //Get the Video Rotation property from the vertex.
        int videoRotation = 180;
        String[] ffmpegRotationStrings = VideoUtils.createFFMPEGRotationOptions(videoRotation);

        File webmFile = File.createTempFile("encode_webm_", ".webm");
        try {
            processRunner.execute(
                    "ffmpeg",
                    new String[]{
                            "-y", // overwrite output files
                            "-i", data.getLocalFile().getAbsolutePath(),
                            "-vcodec", "libvpx",
                            "-b:v", "600k",
                            "-qmin", "10",
                            "-qmax", "42",
                            "-maxrate", "500k",
                            "-bufsize", "1000k",
                            "-threads", "2",
                            "-vf", "scale=720:480",
                            "-vf", "transpose=1,transpose=1",
                            "-acodec", "libvorbis",
                            "-map", "0", // process all streams
                            "-map", "-0:s", // ignore subtitles
                            "-f", "webm",
                            webmFile.getAbsolutePath()
                    },
                    null,
                    data.getLocalFile().getAbsolutePath() + ": "
            );
            //TODO. Should scale always be 720:480?

            ExistingElementMutation<Vertex> m = data.getElement().prepareMutation();

            InputStream webmFileIn = new FileInputStream(webmFile);
            try {
                StreamingPropertyValue spv = new StreamingPropertyValue(webmFileIn, byte[].class);
                spv.searchIndex(false);
                Map<String, Object> metadata = new HashMap<String, Object>();
                metadata.put(RawLumifyProperties.MIME_TYPE.getPropertyName(), MediaLumifyProperties.MIME_TYPE_VIDEO_WEBM);
                MediaLumifyProperties.VIDEO_WEBM.addPropertyValue(m, PROPERTY_KEY, spv, metadata, data.getProperty().getVisibility());
                m.save(getAuthorizations());
            } finally {
                webmFileIn.close();
            }
        } finally {
            webmFile.delete();
        }
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        if (!property.getName().equals(RawLumifyProperties.RAW.getPropertyName())) {
            return false;
        }
        String mimeType = (String) property.getMetadata().get(RawLumifyProperties.MIME_TYPE.getPropertyName());
        if (mimeType == null || !mimeType.startsWith("video")) {
            return false;
        }

        if (MediaLumifyProperties.VIDEO_WEBM.hasProperty(element, PROPERTY_KEY)) {
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
