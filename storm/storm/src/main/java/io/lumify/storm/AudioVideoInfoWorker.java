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

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AudioVideoInfoWorker extends GraphPropertyWorker {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(AudioVideoInfoWorker.class);
    private static final String PROPERTY_KEY = AudioVideoInfoWorker.class.getName();
    private ProcessRunner processRunner;
    private MediaPropertyConfiguration config = new MediaPropertyConfiguration();

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);
        getConfiguration().setConfigurables(config, MediaPropertyConfiguration.PROPERTY_NAME_PREFIX);
    }

    private void setProperty(String iri, Object value, ExistingElementMutation<Vertex> mutation, Map<String, Object> metadata, GraphPropertyWorkData data, List<String> properties) {
        if (iri != null && value != null) {
            mutation.addPropertyValue(PROPERTY_KEY, iri, value, metadata, data.getVisibility());
            properties.add(iri);
        }
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        LOGGER.debug("BEGIN executing worker for element %s", data.getElement().getId());
        File localFile = data.getLocalFile();

        Map<String, Object> metadata = data.createPropertyMetadata();
        ExistingElementMutation<Vertex> mutation = data.getElement().prepareMutation();
        List<String> properties = new ArrayList<String>();

        JSONObject json = FFprobeExecutor.getJson(processRunner, data);
        if (json != null) {
            setProperty(config.getDurationIri(), DurationUtil.extractDurationFromJSON(json), mutation, metadata, data, properties);
            setProperty(config.getGeoLocationIri(), GeoLocationUtil.extractGeoLocationFromJSON(json), mutation, metadata, data, properties);
            setProperty(config.getDateTakenIri(), DateUtil.extractDateTakenFromJSON(json), mutation, metadata, data, properties);
            setProperty(config.getDeviceMakeIri(), MakeAndModelUtil.extractMakeFromJSON(json), mutation, metadata, data, properties);
            setProperty(config.getDeviceModelIri(), MakeAndModelUtil.extractModelFromJSON(json), mutation, metadata, data, properties);
            setProperty(config.getWidthIri(), DimensionsUtil.extractWidthFromJSON(json), mutation, metadata, data, properties);
            setProperty(config.getHeightIri(), DimensionsUtil.extractHeightFromJSON(json), mutation, metadata, data, properties);
            setProperty(config.getMetadataIri(), json.toString(), mutation, metadata, data, properties);
            setProperty(config.getClockwiseRotationIri(), VideoRotationUtil.extractRotationFromJSON(json), mutation, metadata, data, properties);
        }

        setProperty(config.getFileSizeIri(), FileSizeUtil.extractFileSize(localFile), mutation, metadata, data, properties);

        mutation.save(getAuthorizations());
        getGraph().flush();
        for (String propertyName : properties) {
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

        LOGGER.debug("handling element %s, property %s", element.getId(), property.getName());
        return true;
    }

    @Override
    public boolean isLocalFileRequired() {
        return true;
    }

    @Inject
    public void setProcessRunner(ProcessRunner processRunner) {
        this.processRunner = processRunner;
    }
}
