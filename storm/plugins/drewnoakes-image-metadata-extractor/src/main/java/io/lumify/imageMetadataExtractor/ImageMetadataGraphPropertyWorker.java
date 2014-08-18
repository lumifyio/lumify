package io.lumify.imageMetadataExtractor;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.imageMetadataHelper.*;
import io.lumify.storm.util.FileSizeUtil;
import org.json.JSONObject;
import org.securegraph.Element;
import org.securegraph.Property;
import org.securegraph.Vertex;
import org.securegraph.mutation.ExistingElementMutation;
import org.securegraph.type.GeoPoint;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;


public class ImageMetadataGraphPropertyWorker extends GraphPropertyWorker {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ImageMetadataGraphPropertyWorker.class);
    private static final String MULTI_VALUE_KEY = ImageMetadataGraphPropertyWorker.class.getName();

    @Override
    public boolean isLocalFileRequired() {
        return true;
    }

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        Map<String, Object> metadata = data.createPropertyMetadata();
        ExistingElementMutation<Vertex> m = data.getElement().prepareMutation();
        ArrayList<String> propertiesToQueue = new ArrayList<String>();
        File imageFile = data.getLocalFile();
        if (imageFile != null) {
            Metadata imageMetadata = null;
            try {
                imageMetadata = ImageMetadataReader.readMetadata(imageFile);
            } catch (ImageProcessingException e) {
                LOGGER.debug("Could not read metadata from imageFile.");
            } catch (IOException e) {
                LOGGER.debug("Could not read metadata from imageFile.");
            }
            if (imageMetadata != null) {
                Date dateTaken = DateExtractor.getDateDefault(imageMetadata);
                if (dateTaken != null) {
                    m.addPropertyValue(MULTI_VALUE_KEY, Ontology.dateTakenIri, dateTaken, metadata, data.getVisibility());
                    propertiesToQueue.add(Ontology.dateTakenIri);
                }

                String deviceMake = MakeExtractor.getMake(imageMetadata);
                if (deviceMake != null) {
                    m.addPropertyValue(MULTI_VALUE_KEY, Ontology.deviceMakeIri, deviceMake, metadata, data.getVisibility());
                    propertiesToQueue.add(Ontology.deviceMakeIri);
                }

                String deviceModel = ModelExtractor.getModel(imageMetadata);
                if (deviceModel != null) {
                    m.addPropertyValue(MULTI_VALUE_KEY, Ontology.deviceModelIri, deviceModel, metadata, data.getVisibility());
                    propertiesToQueue.add(Ontology.deviceModelIri);
                }

                GeoPoint geoLocation = GeoPointExtractor.getGeoPoint(imageMetadata);
                if (geoLocation != null) {
                    m.addPropertyValue(MULTI_VALUE_KEY, Ontology.geoLocationIri, geoLocation, metadata, data.getVisibility());
                    propertiesToQueue.add(Ontology.geoLocationIri);
                }

                Double heading = HeadingExtractor.getImageHeading(imageMetadata);
                if (heading != null) {
                    m.addPropertyValue(MULTI_VALUE_KEY, Ontology.headingIri, heading, metadata, data.getVisibility());
                    propertiesToQueue.add(Ontology.headingIri);
                }

                JSONObject imageMetadataJSON = LeftoverMetadataExtractor.getAsJSON(imageMetadata);
                if (imageMetadataJSON != null) {
                    String imageMetadataJSONString = imageMetadataJSON.toString();
                    if (imageMetadataJSONString != null) {
                        m.addPropertyValue(MULTI_VALUE_KEY, Ontology.metadataIri, imageMetadataJSONString, metadata, data.getVisibility());
                        propertiesToQueue.add(Ontology.metadataIri);
                    }
                }
            }

            Integer width = null;
            if (imageMetadata != null) {
                width = DimensionsExtractor.getWidthViaMetadata(imageMetadata);
            }
            if (width != null) {
                width = DimensionsExtractor.getWidthViaBufferedImage(imageFile);
            }
            if (width != null) {
                m.addPropertyValue(MULTI_VALUE_KEY, Ontology.widthIri, width, metadata, data.getVisibility());
                propertiesToQueue.add(Ontology.widthIri);
            }

            Integer height = null;
            if (imageMetadata != null) {
                height = DimensionsExtractor.getHeightViaMetadata(imageMetadata);
            }
            if (height != null) {
                height = DimensionsExtractor.getHeightViaBufferedImage(imageFile);
            }
            if (height != null) {
                m.addPropertyValue(MULTI_VALUE_KEY, Ontology.heightIri, height, metadata, data.getVisibility());
                propertiesToQueue.add(Ontology.heightIri);
            }

            Integer fileSize = FileSizeUtil.extractFileSize(imageFile);
            if (fileSize != null) {
                m.addPropertyValue(MULTI_VALUE_KEY, Ontology.fileSizeIri, fileSize, metadata, data.getVisibility());
                propertiesToQueue.add(Ontology.fileSizeIri);
            }

        }

        m.save(getAuthorizations());
        getGraph().flush();
        for (String propertyName : propertiesToQueue) {
            getWorkQueueRepository().pushGraphPropertyQueue(data.getElement(), MULTI_VALUE_KEY, propertyName);
        }
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        String mimeType = (String) property.getMetadata().get(LumifyProperties.MIME_TYPE.getPropertyName());
        if (mimeType == null) {
            return false;
        }

        if (mimeType.startsWith("image")) {
            return true;
        } else {
            return false;
        }
    }

}
