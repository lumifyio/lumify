package io.lumify.storm;

import com.google.inject.Inject;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.core.util.ProcessRunner;
import org.json.JSONObject;
import org.securegraph.Element;
import org.securegraph.Property;
import org.securegraph.Vertex;
import org.securegraph.mutation.ExistingElementMutation;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;

public class AudioVideoInfoWorker extends GraphPropertyWorker {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(AudioVideoInfoWorker.class);
    private static final String PROPERTY_KEY = AudioVideoInfoWorker.class.getName();
    private ProcessRunner processRunner;
    private static final String AUDIO_DURATION_IRI = "ontology.iri.audioDuration";
    private static final String VIDEO_DURATION_IRI = "ontology.iri.videoDuration";
    private String audioDurationIri;
    private String videoDurationIri;

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);

        audioDurationIri = (String) workerPrepareData.getStormConf().get(AUDIO_DURATION_IRI);
        if (audioDurationIri == null || audioDurationIri.length() == 0) {
            LOGGER.warn("Could not find config: " + AUDIO_DURATION_IRI + ": skipping audio duration extraction.");
        }

        videoDurationIri = (String) workerPrepareData.getStormConf().get(VIDEO_DURATION_IRI);
        if (videoDurationIri == null || videoDurationIri.length() == 0) {
            LOGGER.warn("Could not find config: " + VIDEO_DURATION_IRI + ": skipping video duration extraction.");
        }
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        String mimeType = (String) data.getProperty().getMetadata().get(LumifyProperties.MIME_TYPE.getPropertyName());
        boolean isAudio = mimeType.startsWith("audio");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        processRunner.execute(
                "ffprobe",
                new String[]{
                        "-v", "quiet",
                        "-print_format", "json",
                        "-show_format",
                        "-show_streams",
                        data.getLocalFile().getAbsolutePath()
                },
                out,
                data.getLocalFile().getAbsolutePath() + ": "
        );
        String outString = new String(out.toByteArray());
        JSONObject outJson = new JSONObject(outString);
        LOGGER.debug("info for %s:\n%s", data.getLocalFile().getAbsolutePath(), outJson.toString(2));

        JSONObject formatJson = outJson.optJSONObject("format");

        Double duration = null;
        if (formatJson != null) {
            duration = formatJson.optDouble("duration");
        }

        Map<String, Object> metadata = data.createPropertyMetadata();
        ExistingElementMutation<Vertex> m = data.getElement().prepareMutation();

        String durationIri = isAudio ? audioDurationIri : videoDurationIri;
        if (duration != null) {
            m.addPropertyValue(PROPERTY_KEY, durationIri, duration, metadata, data.getVisibility());
        }

        m.save(getAuthorizations());
        getGraph().flush();

        if (duration != null) {
            getWorkQueueRepository().pushGraphPropertyQueue(data.getElement(), PROPERTY_KEY, durationIri);
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
        if (mimeType == null || !(mimeType.startsWith("video") || mimeType.startsWith("audio"))) {
            return false;
        }

        if (mimeType.startsWith("video") && videoDurationIri == null) {
            return false;
        }

        if (mimeType.startsWith("audio") && audioDurationIri == null) {
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
