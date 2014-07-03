package io.lumify.imageMetadataExtractor;

import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.PostMimeTypeWorker;
import io.lumify.imageMetadataHelper.ImageTransformExtractor;
import org.securegraph.Authorizations;

import java.io.File;

public class ImageOrientationPostMimeTypeWorker extends PostMimeTypeWorker {
    private static final String MULTI_VALUE_PROPERTY_KEY = ImageOrientationPostMimeTypeWorker.class.getName();

    @Override
    public void execute(String mimeType, GraphPropertyWorkData data, Authorizations authorizations) throws Exception {
        if (!mimeType.startsWith("image")) {
            return;
        }

        File localFile = getLocalFileForRaw(data.getElement());
        ImageTransform imageTransform = ImageTransformExtractor.getImageTransform(localFile);
        if (imageTransform != null) {
            data.getElement().addPropertyValue(
                    MULTI_VALUE_PROPERTY_KEY,
                    Ontology.Y_AXIS_FLIP_NEEDED.getPropertyName(),
                    imageTransform.isYAxisFlipNeeded(),
                    data.getVisibility(),
                    authorizations);

            data.getElement().addPropertyValue(
                    MULTI_VALUE_PROPERTY_KEY,
                    Ontology.CW_ROTATION_NEEDED.getPropertyName(),
                    imageTransform.getCWRotationNeeded(),
                    data.getVisibility(),
                    authorizations);

            getGraph().flush();

            getWorkQueueRepository().pushGraphPropertyQueue(data.getElement(), MULTI_VALUE_PROPERTY_KEY, Ontology.Y_AXIS_FLIP_NEEDED.getPropertyName());
            getWorkQueueRepository().pushGraphPropertyQueue(data.getElement(), MULTI_VALUE_PROPERTY_KEY, Ontology.CW_ROTATION_NEEDED.getPropertyName());
        }
    }

}