package io.lumify.imageMetadataExtractor;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import io.lumify.core.model.properties.RawLumifyProperties;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.imageMetadataHelper.*;
import org.securegraph.Element;
import org.securegraph.Property;
import org.securegraph.type.GeoPoint;

import java.io.File;
import java.io.InputStream;
import java.util.Date;


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
        File imageFile = data.getLocalFile();

        Metadata metadata = ImageMetadataReader.readMetadata(imageFile);

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

        Double imageFacingDirection = DirectionExtractor.getImageFacingDirection(metadata);
        if (imageFacingDirection != null) {
            Ontology.DIRECTION.addPropertyValue(data.getElement(), MULTI_VALUE_KEY, imageFacingDirection, data.getVisibility(), getAuthorizations());
        }

        String imageFacingDirectionDescription = DirectionExtractor.getImageFacingDirectionDescription(metadata);
        if (imageFacingDirectionDescription != null) {
            Ontology.DIRECTION_DESCRIPTION.addPropertyValue(data.getElement(), MULTI_VALUE_KEY, imageFacingDirectionDescription, data.getVisibility(), getAuthorizations());
        }
    }


    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        String mimeType = (String) property.getMetadata().get(RawLumifyProperties.MIME_TYPE.getPropertyName());
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
