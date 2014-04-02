package com.altamiracorp.lumify.core.ingest.video;

import com.altamiracorp.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import com.altamiracorp.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import com.altamiracorp.lumify.core.model.properties.MediaLumifyProperties;
import com.altamiracorp.lumify.core.model.properties.RawLumifyProperties;
import com.altamiracorp.lumify.core.util.ProcessRunner;
import com.altamiracorp.securegraph.Property;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.mutation.ExistingElementMutation;
import com.altamiracorp.securegraph.property.StreamingPropertyValue;
import com.google.inject.Inject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class VideoPosterFrameWorker extends GraphPropertyWorker {
    private static final String PROPERTY_KEY = VideoPosterFrameWorker.class.getName();
    private ProcessRunner processRunner;

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        File videoPosterFrameFile = File.createTempFile("video_poster_frame", ".png");
        try {
            processRunner.execute(
                    "ffmpeg",
                    new String[]{
                            "-itsoffset", "-4",
                            "-i", data.getLocalFile().getAbsolutePath(),
                            "-vcodec", "png",
                            "-vframes", "1",
                            "-an",
                            "-f", "rawvideo",
                            "-s", "720x480",
                            "-y",
                            videoPosterFrameFile.getAbsolutePath()
                    },
                    null,
                    data.getLocalFile().getAbsolutePath() + ": "
            );

            if (videoPosterFrameFile.length() == 0) {
                throw new RuntimeException("Poster frame not created. Zero length file detected. (from: " + data.getLocalFile().getAbsolutePath() + ")");
            }

            InputStream videoPosterFrameFileIn = new FileInputStream(videoPosterFrameFile);
            try {
                ExistingElementMutation<Vertex> m = data.getVertex().prepareMutation();

                StreamingPropertyValue spv = new StreamingPropertyValue(videoPosterFrameFileIn, byte[].class);
                spv.searchIndex(false);
                Map<String, Object> metadata = new HashMap<String, Object>();
                metadata.put(RawLumifyProperties.METADATA_MIME_TYPE, "image/png");
                MediaLumifyProperties.RAW_POSTER_FRAME.addPropertyValue(m, PROPERTY_KEY, spv, metadata, data.getProperty().getVisibility());
                m.save();
                getGraph().flush();
            } finally {
                videoPosterFrameFileIn.close();
            }
        } finally {
            videoPosterFrameFile.delete();
        }
    }

    @Override
    public boolean isHandled(Vertex vertex, Property property) {
        if (!property.getName().equals(RawLumifyProperties.RAW.getKey())) {
            return false;
        }
        String mimeType = (String) property.getMetadata().get(RawLumifyProperties.METADATA_MIME_TYPE);
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
