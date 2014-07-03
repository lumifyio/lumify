package io.lumify.foodTruck;

import io.lumify.core.model.properties.types.DateLumifyProperty;
import io.lumify.core.model.properties.types.GeoPointLumifyProperty;
import io.lumify.core.model.properties.types.StringLumifyProperty;

public class FoodTruckOntology {
    public static final String EDGE_LABEL_HAS_KEYWORD = "http://lumify.io/foodtruck#tweetHasKeyword";
    public static final String EDGE_LABEL_HAS_TWITTER_USER = "http://lumify.io/foodtruck#foodTruckHasTwitterUser";

    public static final String CONCEPT_TYPE_FOOD_TRUCK = "http://lumify.io/foodtruck#foodTruck";
    public static final String CONCEPT_TYPE_LOCATION = "http://lumify.io/foodtruck#location";

    public static final GeoPointLumifyProperty GEO_LOCATION = new GeoPointLumifyProperty("http://lumify.io/foodtruck#geoLocation");
    public static final DateLumifyProperty GEO_LOCATION_DATE = new DateLumifyProperty("http://lumify.io/foodtruck#geoLocationDate");
    public static final StringLumifyProperty KEYWORD = new StringLumifyProperty("http://lumify.io/foodtruck#keyword");
}
