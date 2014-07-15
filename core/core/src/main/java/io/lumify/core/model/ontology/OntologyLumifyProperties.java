package io.lumify.core.model.ontology;

import io.lumify.core.model.properties.types.*;

/**
 * LumifyProperty values used for storage of Ontology concepts.
 */
public final class OntologyLumifyProperties {
    public static final StringLumifyProperty CONCEPT_TYPE = new StringLumifyProperty("http://lumify.io#conceptType");

    public static final StringLumifyProperty DATA_TYPE = new StringLumifyProperty("http://lumify.io#dataType");

    public static final BooleanLumifyProperty USER_VISIBLE = new BooleanLumifyProperty("http://lumify.io#userVisible");

    public static final StringLumifyProperty ONTOLOGY_TITLE = new StringLumifyProperty("http://lumify.io#ontologyTitle");

    public static final BooleanLumifyProperty SEARCHABLE = new BooleanLumifyProperty("http://lumify.io#searchable");

    public static final StringLumifyProperty DISPLAY_TYPE = new StringLumifyProperty("http://lumify.io#displayType");

    public static final StringLumifyProperty TITLE_FORMULA = new StringLumifyProperty("http://lumify.io#titleFormula");

    public static final StringLumifyProperty SUBTITLE_FORMULA = new StringLumifyProperty("http://lumify.io#subtitleFormula");

    public static final StringLumifyProperty TIME_FORMULA = new StringLumifyProperty("http://lumify.io#timeFormula");

    public static final BooleanLumifyProperty DISPLAY_TIME = new BooleanLumifyProperty("http://lumify.io#displayTime");

    public static final DoubleLumifyProperty BOOST = new DoubleLumifyProperty("http://lumify.io#boost");

    public static final StringLumifyProperty COLOR = new StringLumifyProperty("http://lumify.io#color");

    public static final JsonLumifyProperty POSSIBLE_VALUES = new JsonLumifyProperty("http://lumify.io#possibleValues");

    public static final StringLumifyProperty TEXT_INDEX_HINTS = new StringLumifyProperty("http://lumify.io#textIndexHints");

    public static final StringLumifyProperty GLYPH_ICON_FILE_NAME = new StringLumifyProperty("http://lumify.io#glyphIconFileName");

    public static final StringLumifyProperty MAP_GLYPH_ICON_FILE_NAME = new StringLumifyProperty("http://lumify.io#mapGlyphIconFileName");

    public static final StringLumifyProperty ENTITY_HAS_IMAGE_VERTEX_ID = new StringLumifyProperty("http://lumify.io#entityImageVertexId");

    private OntologyLumifyProperties() {
        throw new UnsupportedOperationException("do not construct utility class");
    }
}
