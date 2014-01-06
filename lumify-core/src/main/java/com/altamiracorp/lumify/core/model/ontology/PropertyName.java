package com.altamiracorp.lumify.core.model.ontology;

public enum PropertyName {
    TYPE("_type"),
    SUBTYPE("_subType"),
    DATA_TYPE("_dataType"),
    TITLE("title"),
    ONTOLOGY_TITLE("ontologyTitle"),
    DISPLAY_NAME("displayName"),
    DISPLAY_TYPE("displayType"),
    GEO_LOCATION("geoLocation"),
    GEO_LOCATION_DESCRIPTION("_geoLocationDescription"),
    ROW_KEY("_rowKey"),
    GLYPH_ICON("_glyphIcon"),
    MAP_GLYPH_ICON("_mapGlyphIcon"),
    COLOR("_color"),
    SOURCE("source"),
    START_DATE("startDate"),
    END_DATE("endDate"),
    RELATIONSHIP_TYPE("relationshipType"),
    PUBLISHED_DATE("publishedDate"),
    TIME_STAMP("_timeStamp"),
    RAW_HDFS_PATH("_rawHdfsPath"),
    TEXT_HDFS_PATH("_textHdfsPath"),
    DETECTED_OBJECTS("_detectedObjects"),
    HIGHLIGHTED_TEXT_HDFS_PATH("highlightedTextHdfsPath"),
    AUTHOR("author");

    private final String text;

    PropertyName(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return this.text;
    }
}
