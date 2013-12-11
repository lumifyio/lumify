package com.altamiracorp.lumify.core.model.graph;

public class GraphGeoLocation {
    private final double latitude;
    private final double longitude;

    public GraphGeoLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
