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
    private MediaPropertyConfiguration config = new MediaPropertyConfiguration();

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);
        getConfiguration().setConfigurables(config, "ontology.iri");
    }

    private void setProperty(ExistingElementMutation<Vertex> mutation, ArrayList<String> properties, String iri, Object value, Map<String, Object> metadata, GraphPropertyWorkData data) {
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
        ArrayList<String> properties = new ArrayList<String>();

        JSONObject json = FFprobeExecutor.getJson(processRunner, data);
        if (json != null) {
            setProperty(mutation, properties, config.getDurationIri(), DurationUtil.extractDurationFromJSON(json), metadata, data);
            setProperty(mutation, properties, config.getGeoLocationIri(), GeoLocationUtil.extractGeoLocationFromJSON(json), metadata, data);
            setProperty(mutation, properties, config.getDateTakenIri(), DateUtil.extractDateTakenFromJSON(json), metadata, data);
            setProperty(mutation, properties, config.getDeviceMakeIri(), MakeAndModelUtil.extractMakeFromJSON(json), metadata, data);
            setProperty(mutation, properties, config.getDeviceModelIri(), MakeAndModelUtil.extractModelFromJSON(json), metadata, data);
            setProperty(mutation, properties, config.getWidthIri(), DimensionsUtil.extractWidthFromJSON(json), metadata, data);
            setProperty(mutation, properties, config.getHeightIri(), DimensionsUtil.extractHeightFromJSON(json), metadata, data);
            setProperty(mutation, properties, config.getMetadataIri(), json.toString(), metadata, data);
            setProperty(mutation, properties, config.getVideoRotationIri(), VideoRotationUtil.extractRotationFromJSON(json), metadata, data);
        }

        setProperty(mutation, properties, config.getFileSizeIri(), FileSizeUtil.extractFileSize(localFile), metadata, data);

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
