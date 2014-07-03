package io.lumify.flightTrack;

import io.lumify.core.model.properties.types.DoubleLumifyProperty;
import io.lumify.core.model.properties.types.GeoPointLumifyProperty;
import io.lumify.core.model.properties.types.StringLumifyProperty;

public class FlightTrackOntology {
    public static final String EDGE_LABEL_HAS_ORIGIN = "http://lumify.io/flightTrack#airplaneOrigin";
    public static final String EDGE_LABEL_HAS_AIRPLANE = "http://lumify.io/flightTrack#airlineHasAirplane";
    public static final String EDGE_LABEL_HAS_DESTINATION = "http://lumify.io/flightTrack#airplaneDestination";

    public static final String CONCEPT_TYPE_AIRPORT = "http://lumify.io/flightTrack#airport";
    public static final String CONCEPT_TYPE_AIRPLANE = "http://lumify.io/flightTrack#airplane";
    public static final String CONCEPT_TYPE_FLIGHT_TRACK = "http://lumify.io/flightTrack#flightTrack";
    public static final String CONCEPT_TYPE_AIRLINE = "http://lumify.io/flightTrack#airline";

    public static final StringLumifyProperty IDENT = new StringLumifyProperty("http://lumify.io/flightTrack#ident");
    public static final StringLumifyProperty AIRPORT_CODE = new StringLumifyProperty("http://lumify.io/flightTrack#airportCode");
    public static final DoubleLumifyProperty HEADING = new DoubleLumifyProperty("http://lumify.io/flightTrack#heading");
    public static final StringLumifyProperty AIRLINE_PREFIX = new StringLumifyProperty("http://lumify.io/flightTrack#airlinePrefix");
    public static final DoubleLumifyProperty ALTITUDE = new DoubleLumifyProperty("http://lumify.io/flightTrack#altitude");
    public static final GeoPointLumifyProperty LOCATION = new GeoPointLumifyProperty("http://lumify.io/flightTrack#geoLocation");
}
