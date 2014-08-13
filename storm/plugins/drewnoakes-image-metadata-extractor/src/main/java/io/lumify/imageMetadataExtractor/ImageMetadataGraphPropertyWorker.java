package io.lumify.imageMetadataExtractor;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.imageMetadataHelper.*;
import org.json.JSONObject;
import org.securegraph.Element;
import org.securegraph.Property;
import org.securegraph.type.GeoPoint;

import java.io.File;
import java.io.InputStream;
import java.util.Date;


public class ImageMetadataGraphPropertyWorker extends GraphPropertyWorker {
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
        File imageFile = data.getLocalFile();
        if (imageFile != null) {
            Metadata metadata = ImageMetadataReader.readMetadata(imageFile);
            if (metadata != null) {
                Date date = DateExtractor.getDateDefault(metadata);
                if (date != null) {
                    Ontology.DATE_TAKEN.addPropertyValue(data.getElement(), MULTI_VALUE_KEY, date, data.getVisibility(), getAuthorizations());
                }

                String deviceMake = MakeExtractor.getMake(metadata);
                if (deviceMake != null) {
                    Ontology.DEVICE_MAKE.addPropertyValue(data.getElement(), MULTI_VALUE_KEY, deviceMake, data.getVisibility(), getAuthorizations());
                }

                String deviceModel = ModelExtractor.getModel(metadata);
                if (deviceModel != null) {
                    Ontology.DEVICE_MODEL.addPropertyValue(data.getElement(), MULTI_VALUE_KEY, deviceModel, data.getVisibility(), getAuthorizations());
                }

                GeoPoint imageLocation = GeoPointExtractor.getGeoPoint(metadata);
                if (imageLocation != null) {
                    Ontology.GEO_LOCATION.addPropertyValue(data.getElement(), MULTI_VALUE_KEY, imageLocation, data.getVisibility(), getAuthorizations());
                }

                Double imageFacingDirection = HeadingExtractor.getImageHeading(metadata);
                if (imageFacingDirection != null) {
                    Ontology.HEADING.addPropertyValue(data.getElement(), MULTI_VALUE_KEY, imageFacingDirection, data.getVisibility(), getAuthorizations());
                }

                Integer imageWidth = DimensionsExtractor.getWidth(metadata);
                if (imageWidth != null){
                    Ontology.WIDTH.addPropertyValue(data.getElement(), MULTI_VALUE_KEY, imageWidth, data.getVisibility(), getAuthorizations());
                }

                Integer imageHeight = DimensionsExtractor.getHeight(metadata);
                if (imageHeight != null){
                    Ontology.HEIGHT.addPropertyValue(data.getElement(), MULTI_VALUE_KEY, imageHeight, data.getVisibility(), getAuthorizations());
                }

                double fileSize = imageFile.length();
                if (fileSize != 0){
                    Ontology.FILE_SIZE.addPropertyValue(data.getElement(), MULTI_VALUE_KEY, fileSize, data.getVisibility(), getAuthorizations());
                }


                JSONObject imageMetadataJSON = LeftoverMetadataExtractor.getAsJSON(metadata);
                if (imageMetadataJSON != null) {
                    String imageMetadataJSONString = imageMetadataJSON.toString();
                    if (imageMetadataJSONString != null) {
                        Ontology.METADATA.addPropertyValue(data.getElement(), MULTI_VALUE_KEY, imageMetadataJSONString, data.getVisibility(), getAuthorizations());
                    }
                }
            }
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
        //TODO. Checking for jpg only so far. Need to support other file types.
        if (mimeType.startsWith("image/jpeg")) {
            return true;
        } else {
            return false;
        }
    }

}
