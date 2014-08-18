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

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
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
    private static final String LAST_MODIFY_DATE_IRI = "ontology.iri.lastModifyDate";
    private static final String DATE_TAKEN_IRI = "ontology.iri.dateTaken";
    private static final String DEVICE_MAKE_IRI = "ontology.iri.deviceMake";
    private static final String DEVICE_MODEL_IRI = "ontology.iri.deviceModel";
    private static final String METADATA_IRI = "ontology.iri.metadata";
    private static final String WIDTH_IRI = "ontology.iri.width";
    private static final String HEIGHT_IRI = "ontology.iri.height";
    private static final String FILE_SIZE_IRI = "ontology.iri.fileSize";
    private String audioDurationIri;
    private String videoDurationIri;
    private String videoRotationIri;
    private String geoLocationIri;
    private String lastModifyDateIri;
    private String dateTakenIri;
    private String deviceMakeIri;
    private String deviceModelIri;
    private String metadataIri;
    private String widthIri;
    private String heightIri;
    private String fileSizeIri;

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

        lastModifyDateIri = (String) workerPrepareData.getStormConf().get(LAST_MODIFY_DATE_IRI);
        if (lastModifyDateIri == null || lastModifyDateIri.length() == 0) {
            LOGGER.warn("Could not find config: " + LAST_MODIFY_DATE_IRI + ": skipping setting the lastModifyDate property.");
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
        if (deviceModelIri == null || deviceModelIri.length() == 0) {
            LOGGER.warn("Could not find config: " + DEVICE_MODEL_IRI + ": skipping setting the deviceModel property.");
        }

        metadataIri = (String) workerPrepareData.getStormConf().get(METADATA_IRI);
        if (metadataIri == null || metadataIri.length() == 0) {
            LOGGER.warn("Could not find config: " + METADATA_IRI + ": skipping setting the metadata property.");
        }

        widthIri = (String) workerPrepareData.getStormConf().get(WIDTH_IRI);
        if (widthIri == null || widthIri.length() == 0) {
            LOGGER.warn("Could not find config: " + WIDTH_IRI + ": skipping setting the width property.");
        }

        heightIri = (String) workerPrepareData.getStormConf().get(HEIGHT_IRI);
        if (heightIri == null || heightIri.length() == 0) {
            LOGGER.warn("Could not find config: " + HEIGHT_IRI + ": skipping setting the height property.");
        }

        fileSizeIri = (String) workerPrepareData.getStormConf().get(FILE_SIZE_IRI);
        if (fileSizeIri == null || fileSizeIri.length() == 0) {
            LOGGER.warn("Could not find config: " + FILE_SIZE_IRI + ": skipping setting the size property.");
        }

    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        String mimeType = (String) data.getProperty().getMetadata().get(LumifyProperties.MIME_TYPE.getPropertyName());
        boolean isAudio = mimeType.startsWith("audio");
        File localFile = data.getLocalFile();

        Map<String, Object> metadata = data.createPropertyMetadata();
        ExistingElementMutation<Vertex> m = data.getElement().prepareMutation();
        ArrayList<String> propertiesToQueue = new ArrayList<String>();

        JSONObject json = JSONExtractor.retrieveJSONObjectUsingFFPROBE(processRunner, data);
        if (json != null) {

            String durationIri = isAudio ? audioDurationIri : videoDurationIri;
            Double duration = DurationUtil.extractDurationFromJSON(json);
            if (duration != null) {
                m.addPropertyValue(PROPERTY_KEY, durationIri, duration, metadata, data.getVisibility());
                propertiesToQueue.add(durationIri);
            }

            GeoPoint geoLocation = GeoLocationUtil.extractGeoLocationFromJSON(json);
            if (geoLocation != null) {
                m.addPropertyValue(PROPERTY_KEY, geoLocationIri, geoLocation, metadata, data.getVisibility());
                propertiesToQueue.add(geoLocationIri);
            }

            Date lastModifyDate = DateUtil.extractLastModifyDateFromJSON(json);
            if (lastModifyDate != null) {
                m.addPropertyValue(PROPERTY_KEY, lastModifyDateIri, lastModifyDate, metadata, data.getVisibility());
                propertiesToQueue.add(lastModifyDateIri);
            }

            Date dateTaken = DateUtil.extractDateTakenFromJSON(json);
            if (dateTaken != null) {
                m.addPropertyValue(PROPERTY_KEY, dateTakenIri, dateTaken, metadata, data.getVisibility());
                propertiesToQueue.add(dateTakenIri);
            }

            String deviceMake = MakeAndModelUtil.extractMakeFromJSON(json);
            if (deviceMake != null) {
                m.addPropertyValue(PROPERTY_KEY, deviceMakeIri, deviceMake, metadata, data.getVisibility());
                propertiesToQueue.add(deviceMakeIri);
            }

            String deviceModel = MakeAndModelUtil.extractModelFromJSON(json);
            if (deviceModel != null) {
                m.addPropertyValue(PROPERTY_KEY, deviceModelIri, deviceModel, metadata, data.getVisibility());
                propertiesToQueue.add(deviceModelIri);
            }

            Integer width = DimensionsUtil.extractWidthFromJSON(json);
            if (width != null) {
                m.addPropertyValue(PROPERTY_KEY, widthIri, width, metadata, data.getVisibility());
                propertiesToQueue.add(widthIri);
            }

            Integer height = DimensionsUtil.extractHeightFromJSON(json);
            if (height != null) {
                m.addPropertyValue(PROPERTY_KEY, heightIri, height, metadata, data.getVisibility());
                propertiesToQueue.add(heightIri);
            }

            String videoMetadataJSONString = json.toString();
            if (videoMetadataJSONString != null) {
                m.addPropertyValue(PROPERTY_KEY, metadataIri, videoMetadataJSONString, metadata, data.getVisibility());
                propertiesToQueue.add(metadataIri);
            }
        }

        //Always add a videoRotation property, regardless of whether there is a json or not.
        int videoRotation = 0;
        if (json != null) {
            Integer nullableRotation = VideoRotationUtil.extractRotationFromJSON(json);
            if (nullableRotation != null) {
                videoRotation = nullableRotation;
            }
        }
        m.addPropertyValue(PROPERTY_KEY, videoRotationIri, videoRotation, metadata, data.getVisibility());
        propertiesToQueue.add(videoRotationIri);

        Integer fileSize = FileSizeUtil.extractFileSize(localFile);
        if (fileSize != null) {
            m.addPropertyValue(PROPERTY_KEY, fileSizeIri, fileSize, metadata, data.getVisibility());
            propertiesToQueue.add(fileSizeIri);
        }

        m.save(getAuthorizations());
        getGraph().flush();
        for (String propertyName : propertiesToQueue) {
            getWorkQueueRepository().pushGraphPropertyQueue(data.getElement(), PROPERTY_KEY, propertyName);
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
