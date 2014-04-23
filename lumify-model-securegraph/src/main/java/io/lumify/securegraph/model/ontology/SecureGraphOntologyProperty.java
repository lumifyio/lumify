package io.lumify.securegraph.model.ontology;

import io.lumify.core.model.ontology.OntologyProperty;
import io.lumify.core.model.ontology.PropertyType;
import org.securegraph.Vertex;

import static io.lumify.core.model.ontology.OntologyLumifyProperties.*;
import static io.lumify.core.model.properties.LumifyProperties.DISPLAY_NAME;

public class SecureGraphOntologyProperty extends OntologyProperty {
    private final Vertex vertex;

    public SecureGraphOntologyProperty(Vertex vertex) {
        this.vertex = vertex;
    }

    public String getTitle() {
        return ONTOLOGY_TITLE.getPropertyValue(vertex);
    }

    public String getDisplayName() {
        return DISPLAY_NAME.getPropertyValue(vertex);
    }

    public boolean getUserVisible() {
        return USER_VISIBLE.getPropertyValue(vertex);
    }

    public PropertyType getDataType() {
        return PropertyType.convert(DATA_TYPE.getPropertyValue(vertex));
    }

    public Vertex getVertex() {
        return this.vertex;
    }
}
