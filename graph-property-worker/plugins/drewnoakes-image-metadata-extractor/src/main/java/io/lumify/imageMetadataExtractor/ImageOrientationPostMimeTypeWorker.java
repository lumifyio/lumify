package io.lumify.imageMetadataExtractor;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.PostMimeTypeWorker;
import io.lumify.imageMetadataHelper.ImageTransformExtractor;
import io.lumify.gpw.MediaPropertyConfiguration;
import org.securegraph.Authorizations;
import org.securegraph.Vertex;
import org.securegraph.mutation.ExistingElementMutation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ImageOrientationPostMimeTypeWorker extends PostMimeTypeWorker {
    private static final String MULTI_VALUE_PROPERTY_KEY = ImageOrientationPostMimeTypeWorker.class.getName();
    private MediaPropertyConfiguration config = new MediaPropertyConfiguration();

    private void setProperty(String iri, Object value, ExistingElementMutation<Vertex> mutation, Map<String, Object> metadata, GraphPropertyWorkData data, List<String> properties) {
        if (iri != null && value != null) {
            mutation.addPropertyValue(MULTI_VALUE_PROPERTY_KEY, iri, value, metadata, data.getVisibility());
            properties.add(iri);
        }
    }

    @Override
    public void execute(String mimeType, GraphPropertyWorkData data, Authorizations authorizations) throws Exception {
        if (!mimeType.startsWith("image")) {
            return;
        }

        File localFile = getLocalFileForRaw(data.getElement());
        Map<String, Object> metadata = data.createPropertyMetadata();
        ExistingElementMutation<Vertex> mutation = data.getElement().prepareMutation();
        ArrayList<String> properties = new ArrayList<String>();

        ImageTransform imageTransform = ImageTransformExtractor.getImageTransform(localFile);
        if (imageTransform != null) {
            setProperty(config.yAxisFlippedIri, imageTransform.isYAxisFlipNeeded(), mutation, metadata, data, properties);
            setProperty(config.clockwiseRotationIri, imageTransform.getCWRotationNeeded(), mutation, metadata, data, properties);

            mutation.save(authorizations);
            getGraph().flush();
            for (String propertyName : properties) {
                getWorkQueueRepository().pushGraphPropertyQueue(data.getElement(), MULTI_VALUE_PROPERTY_KEY, propertyName);
            }
        }
    }

    @Inject
    public void setConfiguration(Configuration configuration) {
        configuration.setConfigurables(config, MediaPropertyConfiguration.PROPERTY_NAME_PREFIX);
    }
}
