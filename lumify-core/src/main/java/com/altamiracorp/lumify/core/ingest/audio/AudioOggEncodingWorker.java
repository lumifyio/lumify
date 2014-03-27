package com.altamiracorp.lumify.core.ingest.audio;

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

public class AudioOggEncodingWorker extends GraphPropertyWorker {
    private static final String PROPERTY_KEY = AudioOggEncodingWorker.class.getName();
    private ProcessRunner processRunner;

    @Override
    public GraphPropertyWorkResult execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        File mp4File = File.createTempFile("encode_ogg_", ".ogg");
        try {
            processRunner.execute(
                    "ffmpeg",
                    new String[]{
                            "-y", // overwrite output files
                            "-i", data.getLocalFile().getAbsolutePath(),
                            "-acodec", "libvorbis",
                            mp4File.getAbsolutePath()
                    },
                    null,
                    data.getLocalFile().getAbsolutePath() + ": "
            );

            ExistingElementMutation<Vertex> m = data.getVertex().prepareMutation();

            InputStream mp4FileIn = new FileInputStream(mp4File);
            try {
                StreamingPropertyValue spv = new StreamingPropertyValue(mp4FileIn, byte[].class);
                spv.searchIndex(false);
                getAudioProperty(AUDIO_TYPE_OGG).addPropertyValue(m, PROPERTY_KEY, spv, data.getProperty().getVisibility());
                getAudioSizeProperty(AUDIO_TYPE_OGG).addPropertyValue(m, PROPERTY_KEY, mp4File.length(), data.getProperty().getVisibility());

                m.save();
                getGraph().flush();
                getWorkQueueRepository().pushGraphPropertyQueue(data.getVertex().getId(), PROPERTY_KEY, getAudioProperty(AUDIO_TYPE_OGG).getKey());
                getWorkQueueRepository().pushGraphPropertyQueue(data.getVertex().getId(), PROPERTY_KEY, getAudioSizeProperty(AUDIO_TYPE_OGG).getKey());
            } finally {
                mp4FileIn.close();
            }

            return new GraphPropertyWorkResult();
        } finally {
            mp4File.delete();
        }
    }

    @Override
    public boolean isHandled(Vertex vertex, Property property) {
        if (!property.getName().equals(RawLumifyProperties.RAW.getKey())) {
            return false;
        }

        String mimeType = RawLumifyProperties.MIME_TYPE.getPropertyValue(vertex);
        if (mimeType == null || !mimeType.startsWith("audio")) {
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