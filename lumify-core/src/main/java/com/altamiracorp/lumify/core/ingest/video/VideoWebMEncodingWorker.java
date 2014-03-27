package com.altamiracorp.lumify.core.ingest.video;

import com.altamiracorp.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import com.altamiracorp.lumify.core.ingest.graphProperty.GraphPropertyWorkResult;
import com.altamiracorp.lumify.core.ingest.graphProperty.GraphPropertyWorker;
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

import static com.altamiracorp.lumify.core.model.properties.MediaLumifyProperties.*;

public class VideoWebMEncodingWorker extends GraphPropertyWorker {
    private static final String PROPERTY_KEY = VideoWebMEncodingWorker.class.getName();
    private ProcessRunner processRunner;

    @Override
    public GraphPropertyWorkResult execute(InputStream in, GraphPropertyWorkData data) throws Exception {
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
                            "-acodec", "libvorbis",
                            "-map", "0", // process all streams
                            "-map", "-0:s", // ignore subtitles
                            "-f", "webm",
                            webmFile.getAbsolutePath()
                    },
                    null,
                    data.getLocalFile().getAbsolutePath() + ": "
            );

            ExistingElementMutation<Vertex> m = data.getVertex().prepareMutation();

            InputStream webmFileIn = new FileInputStream(webmFile);
            try {
                StreamingPropertyValue spv = new StreamingPropertyValue(webmFileIn, byte[].class);
                spv.searchIndex(false);
                getVideoProperty(VIDEO_TYPE_WEBM).addPropertyValue(m, PROPERTY_KEY, spv, data.getProperty().getVisibility());
                getVideoSizeProperty(VIDEO_TYPE_WEBM).addPropertyValue(m, PROPERTY_KEY, webmFile.length(), data.getProperty().getVisibility());

                m.save();
                getGraph().flush();
                getWorkQueueRepository().pushGraphPropertyQueue(data.getVertex().getId(), PROPERTY_KEY, getVideoProperty(VIDEO_TYPE_WEBM).getKey());
                getWorkQueueRepository().pushGraphPropertyQueue(data.getVertex().getId(), PROPERTY_KEY, getVideoSizeProperty(VIDEO_TYPE_WEBM).getKey());
            } finally {
                webmFileIn.close();
            }

            return new GraphPropertyWorkResult();
        } finally {
            webmFile.delete();
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
