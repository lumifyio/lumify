package io.lumify.it;

import io.lumify.core.model.properties.types.GeoPointLumifyProperty;

public class TestOntology {
    public static final String CONCEPT_PERSON = "http://lumify.io/test#person";
    public static final String CONCEPT_CITY = "http://lumify.io/test#city";
    public static final String CONCEPT_ZIP_CODE = "http://lumify.io/test#zipCode";

    public static final String EDGE_LABEL_WORKS_FOR = "http://lumify.io/test#worksFor";

    public static final GeoPointLumifyProperty PROPERTY_GEO_LOCATION = new GeoPointLumifyProperty("http://lumify.io/test#geolocation");
    public static final String PROPERTY_NAME = "http://lumify.io/test#name";
}
