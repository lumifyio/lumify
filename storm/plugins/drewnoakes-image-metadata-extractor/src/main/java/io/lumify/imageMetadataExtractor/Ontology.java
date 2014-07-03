package io.lumify.imageMetadataExtractor;

import io.lumify.core.model.properties.types.BooleanLumifyProperty;
import io.lumify.core.model.properties.types.DateLumifyProperty;
import io.lumify.core.model.properties.types.IntegerLumifyProperty;
import io.lumify.core.model.properties.types.StringLumifyProperty;

public class Ontology {

    public static final IntegerLumifyProperty CW_ROTATION_NEEDED = new IntegerLumifyProperty("http://lumify.io/exif#cwrotationneeded");
    public static final BooleanLumifyProperty Y_AXIS_FLIP_NEEDED = new BooleanLumifyProperty("http://lumify.io/exif#yaxisflipneeded");
    public static final DateLumifyProperty DATE_TAKEN = new DateLumifyProperty("http://lumify.io/exif#datetaken");
    public static final StringLumifyProperty DEVICE_MAKE = new StringLumifyProperty("http://lumify.io/exif#devicemake");
    public static final StringLumifyProperty DEVICE_MODEL = new StringLumifyProperty("http://lumify.io/exif#devicemodel");

}