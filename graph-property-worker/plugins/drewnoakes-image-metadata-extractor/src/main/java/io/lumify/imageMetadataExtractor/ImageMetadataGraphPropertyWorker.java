package io.lumify.imageMetadataExtractor;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.gpw.MediaPropertyConfiguration;
import io.lumify.gpw.util.FileSizeUtil;
import io.lumify.imageMetadataHelper.*;
import org.securegraph.Element;
import org.securegraph.Property;
import org.securegraph.Vertex;
import org.securegraph.mutation.ExistingElementMutation;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class ImageMetadataGraphPropertyWorker extends GraphPropertyWorker {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ImageMetadataGraphPropertyWorker.class);
    private static final String MULTI_VALUE_KEY = ImageMetadataGraphPropertyWorker.class.getName();
    private MediaPropertyConfiguration config = new MediaPropertyConfiguration();

    @Override
    public boolean isLocalFileRequired() {
        return true;
    }

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);
        getConfiguration().setConfigurables(config, MediaPropertyConfiguration.PROPERTY_NAME_PREFIX);
    }

    private void setProperty(String iri, Object value, ExistingElementMutation<Vertex> mutation, org.securegraph.Metadata metadata, GraphPropertyWorkData data, List<String> properties) {
        if (iri != null && value != null) {
            mutation.addPropertyValue(MULTI_VALUE_KEY, iri, value, metadata, data.getVisibility());
            properties.add(iri);
        }
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        org.securegraph.Metadata metadata = data.createPropertyMetadata();
        ExistingElementMutation<Vertex> mutation = data.getElement().prepareMutation();
        List<String> properties = new ArrayList<String>();

        File imageFile = data.getLocalFile();
        if (imageFile != null) {
            Metadata imageMetadata = null;
            try {
                imageMetadata = ImageMetadataReader.readMetadata(imageFile);
            } catch (Exception e) {
                LOGGER.error("Could not read metadata from imageFile: %s", imageFile, e);
            }

            if (imageMetadata != null) {
                setProperty(config.dateTakenIri, DateExtractor.getDateDefault(imageMetadata), mutation, metadata, data, properties);
                setProperty(config.deviceMakeIri, MakeExtractor.getMake(imageMetadata), mutation, metadata, data, properties);
                setProperty(config.deviceModelIri, ModelExtractor.getModel(imageMetadata), mutation, metadata, data, properties);
                setProperty(config.geoLocationIri, GeoPointExtractor.getGeoPoint(imageMetadata), mutation, metadata, data, properties);
                setProperty(config.headingIri, HeadingExtractor.getImageHeading(imageMetadata), mutation, metadata, data, properties);
                setProperty(config.metadataIri, LeftoverMetadataExtractor.getAsJSON(imageMetadata).toString(), mutation, metadata, data, properties);
            }

            Integer width = imageMetadata != null ? DimensionsExtractor.getWidthViaMetadata(imageMetadata) : DimensionsExtractor.getWidthViaBufferedImage(imageFile);
            setProperty(config.widthIri, width, mutation, metadata, data, properties);

            Integer height = imageMetadata != null ? DimensionsExtractor.getHeightViaMetadata(imageMetadata) : DimensionsExtractor.getHeightViaBufferedImage(imageFile);
            setProperty(config.heightIri, height, mutation, metadata, data, properties);

            setProperty(config.fileSizeIri, FileSizeUtil.getSize(imageFile), mutation, metadata, data, properties);
        }

        mutation.save(getAuthorizations());
        getGraph().flush();
        for (String propertyName : properties) {
            getWorkQueueRepository().pushGraphPropertyQueue(data.getElement(), MULTI_VALUE_KEY, propertyName);
        }
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        String mimeType = LumifyProperties.MIME_TYPE.getMetadataValue(property.getMetadata(), null);
        if (mimeType != null && (
                mimeType.startsWith("image/png") ||
                        mimeType.startsWith("image/jpeg") ||
                        mimeType.startsWith("image/tiff"))) {
            return true;
        }

        return false;
    }
}
