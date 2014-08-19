package io.lumify.imageMetadataExtractor;

import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.PostMimeTypeWorker;
import io.lumify.imageMetadataHelper.ImageTransformExtractor;
import org.securegraph.Authorizations;
import org.securegraph.Vertex;
import org.securegraph.mutation.ExistingElementMutation;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

public class ImageOrientationPostMimeTypeWorker extends PostMimeTypeWorker {
    private static final String MULTI_VALUE_PROPERTY_KEY = ImageOrientationPostMimeTypeWorker.class.getName();

    @Override
    public void execute(String mimeType, GraphPropertyWorkData data, Authorizations authorizations) throws Exception {
        if (!mimeType.startsWith("image")) {
            return;
        }

        File localFile = getLocalFileForRaw(data.getElement());
        Map<String, Object> metadata = data.createPropertyMetadata();
        ExistingElementMutation<Vertex> m = data.getElement().prepareMutation();
        ArrayList<String> propertiesToQueue = new ArrayList<String>();

        ImageTransform imageTransform = ImageTransformExtractor.getImageTransform(localFile);
        if (imageTransform != null) {
            boolean yAxisFlipNeeded = imageTransform.isYAxisFlipNeeded();
            m.addPropertyValue(MULTI_VALUE_PROPERTY_KEY, Ontology.yAxisFlipNeededIri, yAxisFlipNeeded, metadata, data.getVisibility());
            propertiesToQueue.add(Ontology.yAxisFlipNeededIri);

            int cwRotationNeeded = imageTransform.getCWRotationNeeded();
            m.addPropertyValue(MULTI_VALUE_PROPERTY_KEY, Ontology.cwRotationNeededIri, cwRotationNeeded, metadata, data.getVisibility());
            propertiesToQueue.add(Ontology.cwRotationNeededIri);

            m.save(authorizations);
            getGraph().flush();
            for (String propertyName : propertiesToQueue) {
                getWorkQueueRepository().pushGraphPropertyQueue(data.getElement(), MULTI_VALUE_PROPERTY_KEY, propertyName);
            }
        }
    }

}