package io.lumify.core.model.ontology;

import io.lumify.core.model.properties.types.BooleanLumifyProperty;
import io.lumify.core.model.properties.types.ByteArrayLumifyProperty;
import io.lumify.core.model.properties.types.TextLumifyProperty;
import org.securegraph.TextIndexHint;

/**
 * LumifyProperty values used for storage of Ontology concepts.
 */
public final class OntologyLumifyProperties {
    public static final TextLumifyProperty CONCEPT_TYPE = new TextLumifyProperty("http://lumify.io#conceptType", TextIndexHint.EXACT_MATCH);

    public static final TextLumifyProperty DATA_TYPE = new TextLumifyProperty("http://lumify.io#dataType", TextIndexHint.EXACT_MATCH);

    public static final BooleanLumifyProperty USER_VISIBLE = new BooleanLumifyProperty("http://lumify.io#userVisible");

    public static final TextLumifyProperty ONTOLOGY_TITLE = new TextLumifyProperty("http://lumify.io#ontologyTitle", TextIndexHint.EXACT_MATCH);

    public static final TextLumifyProperty DISPLAY_TYPE = new TextLumifyProperty("http://lumify.io#displayType", TextIndexHint.EXACT_MATCH);

    public static final TextLumifyProperty COLOR = new TextLumifyProperty("http://lumify.io#color", TextIndexHint.NONE);

    public static final ByteArrayLumifyProperty POSSIBLE_VALUES = new ByteArrayLumifyProperty("http://lumify.io#possibleValues");

    private OntologyLumifyProperties() {
        throw new UnsupportedOperationException("do not construct utility class");
    }
}
