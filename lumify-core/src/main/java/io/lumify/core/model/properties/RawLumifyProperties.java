package io.lumify.core.model.properties;

import io.lumify.core.model.properties.types.DateLumifyProperty;
import io.lumify.core.model.properties.types.StreamingLumifyProperty;
import io.lumify.core.model.properties.types.StringLumifyProperty;

/**
 * LumifyProperties specific to Raw entities (e.g. documents, images, video, etc.).
 */
public class RawLumifyProperties {
    public static final String META_DATA_LANGUAGE = "http://lumify.io#language";
    public static final String META_DATA_TEXT_DESCRIPTION = "http://lumify.io#textDescription";

    public static final DateLumifyProperty PUBLISHED_DATE = new DateLumifyProperty("http://lumify.io#publishedDate");
    public static final DateLumifyProperty CREATE_DATE = new DateLumifyProperty("http://lumify.io#createDate");
    public static final StringLumifyProperty FILE_NAME = new StringLumifyProperty("http://lumify.io#fileName");
    public static final StringLumifyProperty FILE_NAME_EXTENSION = new StringLumifyProperty("http://lumify.io#fileNameExtension");
    public static final StringLumifyProperty MIME_TYPE = new StringLumifyProperty("http://lumify.io#mimeType");
    public static final StringLumifyProperty AUTHOR = new StringLumifyProperty("http://lumify.io#author");
    public static final StreamingLumifyProperty RAW = new StreamingLumifyProperty("http://lumify.io#raw");
    public static final StreamingLumifyProperty TEXT = new StreamingLumifyProperty("http://lumify.io#text");
    public static final StreamingLumifyProperty MAPPING_JSON = new StreamingLumifyProperty("http://lumify.io#mappingJson");

    private RawLumifyProperties() {
        throw new UnsupportedOperationException("do not construct utility class");
    }
}
