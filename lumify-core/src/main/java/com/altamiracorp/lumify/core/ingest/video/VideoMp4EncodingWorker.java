package com.altamiracorp.lumify.core.ingest.video;

import com.altamiracorp.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import com.altamiracorp.lumify.core.ingest.graphProperty.GraphPropertyWorkResult;
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

import static com.altamiracorp.lumify.core.model.properties.MediaLumifyProperties.*;

public class VideoMp4EncodingWorker extends GraphPropertyWorker {
    private static final String PROPERTY_KEY = VideoMp4EncodingWorker.class.getName();
    private ProcessRunner processRunner;

    @Override
    public GraphPropertyWorkResult execute(InputStream in, GraphPropertyWorkData data) throws Exception {
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

            ExistingElementMutation<Vertex> m = data.getVertex().prepareMutation();

            InputStream mp4RelocatedFileIn = new FileInputStream(mp4ReloactedFile);
            try {
                StreamingPropertyValue spv = new StreamingPropertyValue(mp4RelocatedFileIn, byte[].class);
                spv.searchIndex(false);
                Map<String, Object> metadata = new HashMap<String, Object>();
                metadata.put(RawLumifyProperties.MIME_TYPE.getKey(), MediaLumifyProperties.MIME_TYPE_VIDEO_MP4);
                VIDEO_MP4.addPropertyValue(m, PROPERTY_KEY, spv, metadata, data.getProperty().getVisibility());
                m.save();
            } finally {
                mp4RelocatedFileIn.close();
            }

            return new GraphPropertyWorkResult();
        } finally {
            mp4File.delete();
            mp4ReloactedFile.delete();
        }
    }

    @Override
    public boolean isHandled(Vertex vertex, Property property) {
        if (!property.getName().equals(RawLumifyProperties.RAW.getKey())) {
            return false;
        }
        String mimeType = RawLumifyProperties.MIME_TYPE.getPropertyValue(vertex);
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
