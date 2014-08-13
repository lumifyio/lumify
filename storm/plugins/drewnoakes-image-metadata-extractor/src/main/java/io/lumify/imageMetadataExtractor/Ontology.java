package io.lumify.imageMetadataExtractor;

import io.lumify.core.model.properties.types.*;

public class Ontology {
    public static final BooleanLumifyProperty Y_AXIS_FLIP_NEEDED = new BooleanLumifyProperty("http://lumify.io/exif#yAxisFlipNeeded");
    public static final IntegerLumifyProperty CW_ROTATION_NEEDED = new IntegerLumifyProperty("http://lumify.io/exif#cwRotationNeeded");
    public static final DoubleLumifyProperty HEADING = new DoubleLumifyProperty("http://lumify.io/exif#heading");

    public static final GeoPointLumifyProperty GEO_LOCATION = new GeoPointLumifyProperty("http://lumify.io/dev#geolocation");
    public static final DateLumifyProperty DATE_TAKEN = new DateLumifyProperty("http://lumify.io/dev#dateTaken");
    public static final StringLumifyProperty DEVICE_MAKE = new StringLumifyProperty("http://lumify.io/dev#deviceMake");
    public static final StringLumifyProperty DEVICE_MODEL = new StringLumifyProperty("http://lumify.io/dev#deviceModel");
    public static final IntegerLumifyProperty WIDTH = new IntegerLumifyProperty("http://lumify.io/dev#width");
    public static final IntegerLumifyProperty HEIGHT = new IntegerLumifyProperty("http://lumify.io/dev#height");
    public static final DoubleLumifyProperty FILE_SIZE = new DoubleLumifyProperty("http://lumify.io/dev#fileSize");
    public static final StringLumifyProperty FORMAT = new StringLumifyProperty("http://lumify.io/dev#format");
    public static final StringLumifyProperty METADATA = new StringLumifyProperty("http://lumify.io/dev#metadata");
}