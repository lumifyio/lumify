package io.lumify.core.model.properties;

import io.lumify.core.model.properties.types.*;

public class LumifyProperties {
    public static final String CONCEPT_TYPE_THING = "http://www.w3.org/2002/07/owl#Thing";
    public static final String META_DATA_LANGUAGE = "http://lumify.io#language";
    public static final String META_DATA_TEXT_DESCRIPTION = "http://lumify.io#textDescription";
    public static final String META_DATA_MIME_TYPE = "http://lumify.io#mimeType";

    public static final BooleanLumifyProperty SEARCHABLE = new BooleanLumifyProperty("http://lumify.io#searchable");
    public static final BooleanLumifyProperty USER_VISIBLE = new BooleanLumifyProperty("http://lumify.io#userVisible");

    public static final DateLumifyProperty CREATE_DATE = new DateLumifyProperty("http://lumify.io#createDate");
    public static final DateLumifyProperty MODIFIED_DATE = new DateLumifyProperty("http://lumify.io#modifiedDate");
    public static final DateLumifyProperty PUBLISHED_DATE = new DateLumifyProperty("http://lumify.io#publishedDate");

    public static final DoubleLumifyProperty BOOST = new DoubleLumifyProperty("http://lumify.io#boost");
    public static final DoubleLumifyProperty CONFIDENCE = new DoubleLumifyProperty("http://lumify.io#confidence");

    public static final JsonLumifyProperty POSSIBLE_VALUES = new JsonLumifyProperty("http://lumify.io#possibleValues");
    public static final VisibilityJsonLumifyProperty VISIBILITY_JSON = new VisibilityJsonLumifyProperty("http://lumify.io#visibilityJson");

    public static final StreamingLumifyProperty GLYPH_ICON = new StreamingLumifyProperty("http://lumify.io#glyphIcon");
    public static final StreamingLumifyProperty MAP_GLYPH_ICON = new StreamingLumifyProperty("http://lumify.io#mapGlyphIcon");
    public static final StreamingLumifyProperty MAPPING_JSON = new StreamingLumifyProperty("http://lumify.io#mappingJson");
    public static final StreamingLumifyProperty METADATA_JSON = new StreamingLumifyProperty("http://lumify.io#metadataJson");
    public static final StreamingLumifyProperty TEXT = new StreamingLumifyProperty("http://lumify.io#text");
    public static final StreamingLumifyProperty RAW = new StreamingLumifyProperty("http://lumify.io#raw");

    public static final JsonArrayLumifyProperty ADD_RELATED_CONCEPT_WHITE_LIST = new JsonArrayLumifyProperty("http://lumify.io#addRelatedConceptWhiteList");
    public static final StringLumifyProperty GRAPH_PROPERTY_WORKER_WHITE_LIST = new StringLumifyProperty("http://lumify.io#graphPropertyWorkerWhiteList");
    public static final StringLumifyProperty GRAPH_PROPERTY_WORKER_BLACK_LIST = new StringLumifyProperty("http://lumify.io#graphPropertyWorkerBlackList");
    public static final StringLumifyProperty AUTHOR = new StringLumifyProperty("http://lumify.io#author");
    public static final StringLumifyProperty COLOR = new StringLumifyProperty("http://lumify.io#color");
    public static final StringLumifyProperty CONCEPT_TYPE = new StringLumifyProperty("http://lumify.io#conceptType");
    public static final StringLumifyProperty CONTENT_HASH = new StringLumifyProperty("http://lumify.io#contentHash");
    public static final StringLumifyProperty DATA_TYPE = new StringLumifyProperty("http://lumify.io#dataType");
    public static final StringLumifyProperty DISPLAY_NAME = new StringLumifyProperty("http://lumify.io#displayName");
    public static final StringLumifyProperty DISPLAY_TYPE = new StringLumifyProperty("http://lumify.io#displayType");
    public static final StringLumifyProperty PROPERTY_GROUP = new StringLumifyProperty("http://lumify.io#propertyGroup");
    public static final StringLumifyProperty FILE_NAME = new StringLumifyProperty("http://lumify.io#fileName");
    public static final StringLumifyProperty FILE_NAME_EXTENSION = new StringLumifyProperty("http://lumify.io#fileNameExtension");
    public static final StringLumifyProperty GLYPH_ICON_FILE_NAME = new StringLumifyProperty("http://lumify.io#glyphIconFileName");
    public static final StringLumifyProperty ENTITY_IMAGE_URL = new StringLumifyProperty("http://lumify.io#entityImageUrl");
    public static final StringLumifyProperty ENTITY_IMAGE_VERTEX_ID = new StringLumifyProperty("http://lumify.io#entityImageVertexId");
    public static final StringLumifyProperty MAP_GLYPH_ICON_FILE_NAME = new StringLumifyProperty("http://lumify.io#mapGlyphIconFileName");
    public static final StringLumifyProperty MIME_TYPE = new StringLumifyProperty("http://lumify.io#mimeType");
    public static final StringLumifyProperty CREATED_BY = new StringLumifyProperty("http://lumify.io#createdBy");
    public static final StringLumifyProperty MODIFIED_BY = new StringLumifyProperty("http://lumify.io#modifiedBy");
    public static final StringLumifyProperty ONTOLOGY_TITLE = new StringLumifyProperty("http://lumify.io#ontologyTitle");
    public static final StringLumifyProperty PROCESS = new StringLumifyProperty("http://lumify.io#process");
    public static final StringLumifyProperty ROW_KEY = new StringLumifyProperty("http://lumify.io#rowKey");
    public static final StringLumifyProperty SOURCE = new StringLumifyProperty("http://lumify.io#source");
    public static final StringLumifyProperty SOURCE_URL = new StringLumifyProperty("http://lumify.io#sourceUrl");
    public static final StringLumifyProperty SUBTITLE_FORMULA = new StringLumifyProperty("http://lumify.io#subtitleFormula");
    public static final StringLumifyProperty TEXT_INDEX_HINTS = new StringLumifyProperty("http://lumify.io#textIndexHints");
    public static final StringLumifyProperty TIME_FORMULA = new StringLumifyProperty("http://lumify.io#timeFormula");
    public static final StringLumifyProperty TITLE = new StringLumifyProperty("http://lumify.io#title");
    public static final StringLumifyProperty TITLE_FORMULA = new StringLumifyProperty("http://lumify.io#titleFormula");
    public static final StringLumifyProperty COMMENT = new StringLumifyProperty("http://lumify.io/comment#entry");

    public static final DetectedObjectProperty DETECTED_OBJECT = new DetectedObjectProperty("http://lumify.io#detectedObject");

    public static final LongLumifyProperty TERM_MENTION_START_OFFSET = new LongLumifyProperty("http://lumify.io/termMention#startOffset");
    public static final LongLumifyProperty TERM_MENTION_END_OFFSET = new LongLumifyProperty("http://lumify.io/termMention#endOffset");
    public static final StringLumifyProperty TERM_MENTION_PROCESS = new StringLumifyProperty("http://lumify.io/termMention#process");
    public static final StringLumifyProperty TERM_MENTION_PROPERTY_KEY = new StringLumifyProperty("http://lumify.io/termMention#propertyKey");
    public static final StringLumifyProperty TERM_MENTION_RESOLVED_EDGE_ID = new StringLumifyProperty("http://lumify.io/termMention#resolvedEdgeId");
    public static final StringLumifyProperty TERM_MENTION_TITLE = new StringLumifyProperty("http://lumify.io/termMention#title");
    public static final StringLumifyProperty TERM_MENTION_CONCEPT_TYPE = new StringLumifyProperty("http://lumify.io/termMention#conceptType");
    public static final VisibilityJsonLumifyProperty TERM_MENTION_VISIBILITY_JSON = new VisibilityJsonLumifyProperty("http://lumify.io/termMention#visibilityJson");
    public static final String TERM_MENTION_LABEL_HAS_TERM_MENTION = "http://lumify.io/termMention#hasTermMention";
    public static final String TERM_MENTION_LABEL_RESOLVED_TO = "http://lumify.io/termMention#resolvedTo";

    private LumifyProperties() {
        throw new UnsupportedOperationException("do not construct utility class");
    }
}
