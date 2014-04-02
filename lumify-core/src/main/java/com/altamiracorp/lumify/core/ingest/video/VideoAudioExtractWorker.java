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

public class VideoAudioExtractWorker extends GraphPropertyWorker {
    private static final String PROPERTY_KEY = VideoAudioExtractWorker.class.getName();
    private ProcessRunner processRunner;

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        File mp3File = File.createTempFile("audio_extract_", ".mp3");
        try {
            processRunner.execute(
                    "ffmpeg",
                    new String[]{
                            "-i", data.getLocalFile().getAbsolutePath(),
                            "-vn",
                            "-ar", "44100",
                            "-ab", "320k",
                            "-f", "mp3",
                            "-y",
                            mp3File.getAbsolutePath()
                    },
                    null,
                    data.getLocalFile().getAbsolutePath() + ": "
            );

            ExistingElementMutation<Vertex> m = data.getVertex().prepareMutation();

            InputStream mp3FileIn = new FileInputStream(mp3File);
            try {
                StreamingPropertyValue spv = new StreamingPropertyValue(mp3FileIn, byte[].class);
                spv.searchIndex(false);
                Map<String, Object> metadata = new HashMap<String, Object>();
                metadata.put(RawLumifyProperties.METADATA_MIME_TYPE, MediaLumifyProperties.MIME_TYPE_AUDIO_MP3);
                MediaLumifyProperties.AUDIO_MP3.addPropertyValue(m, PROPERTY_KEY, spv, metadata, data.getProperty().getVisibility());
                m.save();
                getGraph().flush();

                getWorkQueueRepository().pushGraphPropertyQueue(data.getVertex().getId(), PROPERTY_KEY, MediaLumifyProperties.AUDIO_MP3.getKey());
            } finally {
                mp3FileIn.close();
            }
        } finally {
            mp3File.delete();
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

        if (MediaLumifyProperties.AUDIO_MP3.hasProperty(vertex, PROPERTY_KEY)) {
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
