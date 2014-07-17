package io.lumify.imageMetadataExtractor;

import io.lumify.core.model.properties.types.*;

public class Ontology {

    public static final IntegerLumifyProperty CW_ROTATION_NEEDED = new IntegerLumifyProperty("http://lumify.io/exif#cwRotationNeeded");
    public static final BooleanLumifyProperty Y_AXIS_FLIP_NEEDED = new BooleanLumifyProperty("http://lumify.io/exif#yAxisFlipNeeded");
    public static final DateLumifyProperty DATE_TAKEN = new DateLumifyProperty("http://lumify.io/exif#datetaken");
    public static final StringLumifyProperty DEVICE_MAKE = new StringLumifyProperty("http://lumify.io/exif#devicemake");
    public static final StringLumifyProperty DEVICE_MODEL = new StringLumifyProperty("http://lumify.io/exif#devicemodel");
    public static final GeoPointLumifyProperty GEO_LOCATION = new GeoPointLumifyProperty("http://lumify.io/exif#geoLocation");
    public static final DoubleLumifyProperty DIRECTION = new DoubleLumifyProperty("http://lumify.io/exif#direction");
    public static final StringLumifyProperty DIRECTION_DESCRIPTION = new StringLumifyProperty("http://lumify.io/exif#directionDescription");
}