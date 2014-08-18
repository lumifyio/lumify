package io.lumify.foodTruck;

import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import io.lumify.core.model.properties.LumifyProperties;
import org.securegraph.*;

import java.io.InputStream;

public class FoodTruckHasTwitterAccountOnCreateGraphPropertyWorker extends GraphPropertyWorker {
    private static final String MULTI_VALUE_KEY = FoodTruckHasTwitterAccountOnCreateGraphPropertyWorker.class.getName();

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        Edge hasTwitterUserEdge = (Edge) data.getElement();

        Vertex foodTruckVertex = hasTwitterUserEdge.getVertex(Direction.OUT, getAuthorizations());
        Vertex twitterUserVertex = hasTwitterUserEdge.getVertex(Direction.IN, getAuthorizations());

        String imageVertexId = LumifyProperties.ENTITY_IMAGE_VERTEX_ID.getPropertyValue(twitterUserVertex);
        if (imageVertexId != null && imageVertexId.length() > 0) {
            LumifyProperties.ENTITY_IMAGE_VERTEX_ID.addPropertyValue(foodTruckVertex, MULTI_VALUE_KEY, imageVertexId, new Visibility(data.getVisibilitySource()), getAuthorizations());
            getGraph().flush();
            getWorkQueueRepository().pushGraphPropertyQueue(foodTruckVertex, MULTI_VALUE_KEY, LumifyProperties.ENTITY_IMAGE_VERTEX_ID.getPropertyName());
        }
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (!(element instanceof Edge)) {
            return false;
        }

        Edge edge = (Edge) element;
        if (!edge.getLabel().equals(FoodTruckOntology.EDGE_LABEL_HAS_TWITTER_USER)) {
            return false;
        }

        return true;
    }
}
