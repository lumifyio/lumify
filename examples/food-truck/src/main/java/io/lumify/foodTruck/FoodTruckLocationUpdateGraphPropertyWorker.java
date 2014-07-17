package io.lumify.foodTruck;

import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.twitter.TwitterOntology;
import org.securegraph.*;
import org.securegraph.type.GeoPoint;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;

import static org.securegraph.util.IterableUtils.count;
import static org.securegraph.util.IterableUtils.single;
import static org.securegraph.util.IterableUtils.singleOrDefault;

public class FoodTruckLocationUpdateGraphPropertyWorker extends GraphPropertyWorker {
    private static final String MULTI_VALUE_KEY = FoodTruckLocationUpdateGraphPropertyWorker.class.getName();

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        Edge hasKeywordEdge = (Edge) data.getElement();

        Vertex tweetVertex = hasKeywordEdge.getVertex(Direction.OUT, getAuthorizations());
        if (isRetweet(tweetVertex)) {
            return;
        }

        Vertex keywordVertex = hasKeywordEdge.getVertex(Direction.IN, getAuthorizations());
        Vertex tweeter = single(tweetVertex.getVertices(Direction.BOTH, TwitterOntology.EDGE_LABEL_TWEETED, getAuthorizations()));
        Vertex foodTruck = singleOrDefault(tweeter.getVertices(Direction.BOTH, FoodTruckOntology.EDGE_LABEL_HAS_TWITTER_USER, getAuthorizations()), null);
        if (foodTruck == null) {
            return;
        }

        String keywordTitle = LumifyProperties.TITLE.getPropertyValue(keywordVertex);
        GeoPoint geoLocation = FoodTruckOntology.GEO_LOCATION.getPropertyValue(keywordVertex);
        if (geoLocation != null) {
            Date geoLocationDate = LumifyProperties.PUBLISHED_DATE.getPropertyValue(tweetVertex);
            Date currentGetLocationDate = FoodTruckOntology.GEO_LOCATION_DATE.getPropertyValue(foodTruck);
            if (currentGetLocationDate == null || geoLocationDate.compareTo(currentGetLocationDate) > 0) {
                Calendar geoLocationCalendar = Calendar.getInstance();
                geoLocationCalendar.setTime(geoLocationDate);

                Calendar nowCalendar = Calendar.getInstance();
                nowCalendar.setTime(new Date());

                if (geoLocationCalendar.get(Calendar.DAY_OF_YEAR) != nowCalendar.get(Calendar.DAY_OF_YEAR)) {
                    return;
                }

                geoLocation = new GeoPoint(geoLocation.getLatitude(), geoLocation.getLongitude(), geoLocation.getAltitude(), keywordTitle);
                FoodTruckOntology.GEO_LOCATION.addPropertyValue(foodTruck, MULTI_VALUE_KEY, geoLocation, data.getVisibility(), getAuthorizations());
                FoodTruckOntology.GEO_LOCATION_DATE.addPropertyValue(foodTruck, MULTI_VALUE_KEY, geoLocationDate, data.getVisibility(), getAuthorizations());
                getGraph().flush();
                getWorkQueueRepository().pushGraphPropertyQueue(foodTruck, FoodTruckOntology.GEO_LOCATION.getProperty(foodTruck));
            }
        }
    }

    private boolean isRetweet(Vertex tweetVertex) {
        return count(tweetVertex.getEdges(Direction.IN, TwitterOntology.EDGE_LABEL_RETWEET, getAuthorizations())) > 0;
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (!(element instanceof Edge)) {
            return false;
        }

        Edge edge = (Edge) element;
        if (!edge.getLabel().equals(FoodTruckOntology.EDGE_LABEL_HAS_KEYWORD)) {
            return false;
        }

        return true;
    }
}
