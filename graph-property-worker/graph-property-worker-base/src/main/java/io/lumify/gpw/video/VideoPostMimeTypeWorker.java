package io.lumify.gpw.video;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.PostMimeTypeWorker;
import io.lumify.core.util.ProcessRunner;
import io.lumify.gpw.MediaPropertyConfiguration;
import io.lumify.gpw.util.*;
import org.json.JSONObject;
import org.securegraph.Authorizations;
import org.securegraph.Metadata;
import org.securegraph.Vertex;
import org.securegraph.Visibility;
import org.securegraph.mutation.ExistingElementMutation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class VideoPostMimeTypeWorker extends PostMimeTypeWorker {
    public static final String MULTI_VALUE_PROPERTY_KEY = VideoPostMimeTypeWorker.class.getName();
    private MediaPropertyConfiguration config = new MediaPropertyConfiguration();
    private ProcessRunner processRunner;

    @Override
    public void execute(String mimeType, GraphPropertyWorkData data, Authorizations authorizations) throws Exception {
        if (!mimeType.startsWith("video")) {
            return;
        }

        File localFile = getLocalFileForRaw(data.getElement());
        JSONObject videoMetadata = FFprobeExecutor.getJson(processRunner, localFile.getAbsolutePath());
        ExistingElementMutation<Vertex> m = data.getElement().prepareMutation();
        List<String> properties = new ArrayList<String>();
        Metadata metadata = data.createPropertyMetadata();
        if (videoMetadata != null) {
            setProperty(config.durationIri, FFprobeDurationUtil.getDuration(videoMetadata), m, metadata, data, properties);
            setProperty(config.geoLocationIri, FFprobeGeoLocationUtil.getGeoPoint(videoMetadata), m, metadata, data, properties);
            setProperty(config.dateTakenIri, FFprobeDateUtil.getDateTaken(videoMetadata), m, metadata, data, properties);
            setProperty(config.deviceMakeIri, FFprobeMakeAndModelUtil.getMake(videoMetadata), m, metadata, data, properties);
            setProperty(config.deviceModelIri, FFprobeMakeAndModelUtil.getModel(videoMetadata), m, metadata, data, properties);
            setProperty(config.widthIri, FFprobeDimensionsUtil.getWidth(videoMetadata), m, metadata, data, properties);
            setProperty(config.heightIri, FFprobeDimensionsUtil.getHeight(videoMetadata), m, metadata, data, properties);
            setProperty(config.metadataIri, videoMetadata.toString(), m, metadata, data, properties);
            setProperty(config.clockwiseRotationIri, FFprobeRotationUtil.getRotation(videoMetadata), m, metadata, data, properties);
        }

        setProperty(config.fileSizeIri, FileSizeUtil.getSize(localFile), m, metadata, data, properties);

        m.save(authorizations);
        getGraph().flush();

        for (String propertyName : properties) {
            getWorkQueueRepository().pushGraphPropertyQueue(data.getElement(), MULTI_VALUE_PROPERTY_KEY, propertyName);
        }

        getGraph().flush();
    }

    private void setProperty(String iri, Object value, ExistingElementMutation<Vertex> mutation, Metadata metadata, GraphPropertyWorkData data, List<String> properties) {
        if (iri != null && value != null) {
            mutation.addPropertyValue(MULTI_VALUE_PROPERTY_KEY, iri, value, metadata, new Visibility(data.getVisibilitySource()));
            properties.add(iri);
        }
    }

    @Inject
    public void setProcessRunner(ProcessRunner processRunner) {
        this.processRunner = processRunner;
    }

    @Inject
    public void setConfiguration(Configuration configuration) {
        configuration.setConfigurables(config, MediaPropertyConfiguration.PROPERTY_NAME_PREFIX);
    }
}
