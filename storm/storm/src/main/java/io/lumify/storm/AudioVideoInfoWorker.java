package io.lumify.storm;

import com.google.inject.Inject;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.core.util.ProcessRunner;
import io.lumify.storm.util.*;
import org.json.JSONObject;
import org.securegraph.Element;
import org.securegraph.Property;
import org.securegraph.Vertex;
import org.securegraph.mutation.ExistingElementMutation;
import org.securegraph.type.GeoPoint;

import java.io.InputStream;
import java.util.Date;
import java.util.Map;

public class AudioVideoInfoWorker extends GraphPropertyWorker {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(AudioVideoInfoWorker.class);
    private static final String PROPERTY_KEY = AudioVideoInfoWorker.class.getName();
    private ProcessRunner processRunner;
    private static final String AUDIO_DURATION_IRI = "ontology.iri.audioDuration";
    private static final String VIDEO_DURATION_IRI = "ontology.iri.videoDuration";
    private static final String VIDEO_ROTATION_IRI = "ontology.iri.videoRotation";
    private static final String CONFIG_GEO_LOCATION_IRI = "ontology.iri.geoLocation";
    private static final String DATE_DIGITIZED_IRI = "ontology.iri.dateDigitized";
    private static final String DATE_TAKEN_IRI = "ontology.iri.dateTaken";
    private static final String DEVICE_MAKE_IRI = "ontology.iri.deviceMake";
    private static final String DEVICE_MODEL_IRI = "ontology.iri.deviceModel";
    private String audioDurationIri;
    private String videoDurationIri;
    private String videoRotationIri;
    private String geoLocationIri;
    private String dateDigitizedIri;
    private String dateTakenIri;
    private String deviceMakeIri;
    private String deviceModelIri;

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

        videoRotationIri = (String) workerPrepareData.getStormConf().get(VIDEO_ROTATION_IRI);
        if (videoRotationIri == null || videoRotationIri.length() == 0) {
            LOGGER.warn("Could not find config: " + VIDEO_ROTATION_IRI + ": skipping setting the videoRotation property.");
        }

        geoLocationIri = (String) workerPrepareData.getStormConf().get(CONFIG_GEO_LOCATION_IRI);
        if (geoLocationIri == null || geoLocationIri.length() == 0) {
            LOGGER.warn("Could not find config: " + CONFIG_GEO_LOCATION_IRI + ": skipping setting the geoLocation property.");
        }

        dateDigitizedIri = (String) workerPrepareData.getStormConf().get(DATE_DIGITIZED_IRI);
        if (dateDigitizedIri == null || dateDigitizedIri.length() == 0) {
            LOGGER.warn("Could not find config: " + DATE_DIGITIZED_IRI + ": skipping setting the dateDigitized property.");
        }

        dateTakenIri = (String) workerPrepareData.getStormConf().get(DATE_TAKEN_IRI);
        if (dateTakenIri == null || dateTakenIri.length() == 0) {
            LOGGER.warn("Could not find config: " + DATE_TAKEN_IRI + ": skipping setting the dateTaken property.");
        }

        deviceMakeIri = (String) workerPrepareData.getStormConf().get(DEVICE_MAKE_IRI);
        if (deviceMakeIri == null || deviceMakeIri.length() == 0) {
            LOGGER.warn("Could not find config: " + DEVICE_MAKE_IRI + ": skipping setting the deviceMake property.");
        }

        deviceModelIri = (String) workerPrepareData.getStormConf().get(DEVICE_MODEL_IRI);
        if (deviceModelIri == null || deviceModelIri.length() == 0){
            LOGGER.warn("Could not find config: " + DEVICE_MODEL_IRI + ": skipping setting the deviceModel property.");
        }

    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        String mimeType = (String) data.getProperty().getMetadata().get(LumifyProperties.MIME_TYPE.getPropertyName());
        boolean isAudio = mimeType.startsWith("audio");

        JSONObject json = JSONExtractor.retrieveJSONObjectUsingFFPROBE(processRunner, data);
        Double duration = null;
        if (json != null) {
            duration = DurationUtil.extractDurationFromJSON(json);
        }

        Map<String, Object> metadata = data.createPropertyMetadata();
        ExistingElementMutation<Vertex> m = data.getElement().prepareMutation();

        String durationIri = isAudio ? audioDurationIri : videoDurationIri;
        if (duration != null) {
            m.addPropertyValue(PROPERTY_KEY, durationIri, duration, metadata, data.getVisibility());
        }

        if (json != null) {
            int videoRotation = 0;
            Integer nullableRotation = VideoRotationUtil.extractRotationFromJSON(json);
            if (nullableRotation != null) {
                videoRotation = nullableRotation;
            }
            data.getElement().addPropertyValue(
                    PROPERTY_KEY,
                    videoRotationIri,
                    videoRotation,
                    data.getVisibility(),
                    getAuthorizations());


            GeoPoint geoPoint = GeoLocationUtil.extractGeoLocationFromJSON(json);
            if (geoPoint != null) {
                data.getElement().addPropertyValue(
                        PROPERTY_KEY,
                        geoLocationIri,
                        geoPoint,
                        data.getVisibility(),
                        getAuthorizations()
                );
            }

            Date dateDigitized = DateUtil.extractDateDigitizedFromJSON(json);
            if (dateDigitized != null) {
                data.getElement().addPropertyValue(
                        PROPERTY_KEY,
                        dateDigitizedIri,
                        dateDigitized,
                        data.getVisibility(),
                        getAuthorizations()
                );
            }

            Date dateTaken = DateUtil.extractDateTakenFromJSON(json);
            if (dateTaken != null) {
                data.getElement().addPropertyValue(
                        PROPERTY_KEY,
                        dateTakenIri,
                        dateTaken,
                        data.getVisibility(),
                        getAuthorizations()
                );
            }

            String deviceMake = MakeAndModelUtil.extractMakeFromJSON(json);
            if (deviceMake != null) {
                data.getElement().addPropertyValue(
                        PROPERTY_KEY,
                        deviceMakeIri,
                        deviceMake,
                        data.getVisibility(),
                        getAuthorizations()
                );
            }

            String deviceModel = MakeAndModelUtil.extractModelFromJSON(json);
            if (deviceModel != null){
                data.getElement().addPropertyValue(
                        PROPERTY_KEY,
                        deviceModelIri,
                        deviceModel,
                        data.getVisibility(),
                        getAuthorizations()
                );
            }


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
