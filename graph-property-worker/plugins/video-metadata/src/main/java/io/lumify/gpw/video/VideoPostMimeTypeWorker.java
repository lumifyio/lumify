package io.lumify.gpw.video;

import com.google.inject.Inject;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import io.lumify.core.ingest.graphProperty.PostMimeTypeWorker;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.util.ProcessRunner;
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
    private ProcessRunner processRunner;
    private OntologyRepository ontologyRepository;
    private String durationIri;
    private String geoLocationIri;
    private String dateTakenIri;
    private String deviceMakeIri;
    private String deviceModelIri;
    private String widthIri;
    private String heightIri;
    private String metadataIri;
    private String clockwiseRotationIri;
    private String fileSizeIri;

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);
        durationIri = ontologyRepository.getRequiredPropertyIRIByIntent("media.duration");
        geoLocationIri = ontologyRepository.getRequiredPropertyIRIByIntent("geoLocation");
        dateTakenIri = ontologyRepository.getRequiredPropertyIRIByIntent("media.dateTaken");
        deviceMakeIri = ontologyRepository.getRequiredPropertyIRIByIntent("media.deviceMake");
        deviceModelIri = ontologyRepository.getRequiredPropertyIRIByIntent("media.deviceModel");
        widthIri = ontologyRepository.getRequiredPropertyIRIByIntent("media.width");
        heightIri = ontologyRepository.getRequiredPropertyIRIByIntent("media.height");
        metadataIri = ontologyRepository.getRequiredPropertyIRIByIntent("media.metadata");
        clockwiseRotationIri = ontologyRepository.getRequiredPropertyIRIByIntent("media.clockwiseRotation");
        fileSizeIri = ontologyRepository.getRequiredPropertyIRIByIntent("media.fileSize");
    }

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
            setProperty(durationIri, FFprobeDurationUtil.getDuration(videoMetadata), m, metadata, data, properties);
            setProperty(geoLocationIri, FFprobeGeoLocationUtil.getGeoPoint(videoMetadata), m, metadata, data, properties);
            setProperty(dateTakenIri, FFprobeDateUtil.getDateTaken(videoMetadata), m, metadata, data, properties);
            setProperty(deviceMakeIri, FFprobeMakeAndModelUtil.getMake(videoMetadata), m, metadata, data, properties);
            setProperty(deviceModelIri, FFprobeMakeAndModelUtil.getModel(videoMetadata), m, metadata, data, properties);
            setProperty(widthIri, FFprobeDimensionsUtil.getWidth(videoMetadata), m, metadata, data, properties);
            setProperty(heightIri, FFprobeDimensionsUtil.getHeight(videoMetadata), m, metadata, data, properties);
            setProperty(metadataIri, videoMetadata.toString(), m, metadata, data, properties);
            setProperty(clockwiseRotationIri, FFprobeRotationUtil.getRotation(videoMetadata), m, metadata, data, properties);
        }

        setProperty(fileSizeIri, FileSizeUtil.getSize(localFile), m, metadata, data, properties);

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
    public void setOntologyRepository(OntologyRepository ontologyRepository) {
        this.ontologyRepository = ontologyRepository;
    }
}
