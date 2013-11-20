package com.altamiracorp.lumify.core.model.ontology;

import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.tinkerpop.blueprints.Vertex;

import java.util.Set;

public class VertexConcept extends Concept {
    private final Vertex vertex;

    public VertexConcept(Vertex vertex) {
        this.vertex = vertex;
    }

    public String getId() {
        return getVertex().getId().toString();
    }

    @Override
    public GraphVertex setProperty(String key, Object value) {
        this.vertex.setProperty(key, value);
        return this;
    }

    @Override
    public GraphVertex removeProperty(String key) {
        vertex.removeProperty(key);
        return this;
    }

    @Override
    public Set<String> getPropertyKeys() {
        return this.vertex.getPropertyKeys();
    }

    @Override
    public Object getProperty(String propertyKey) {
        return this.vertex.getProperty(propertyKey);
    }

    public String getTitle() {
        return getVertex().getProperty(PropertyName.ONTOLOGY_TITLE.toString());
    }

    @Override
    public String getGlyphIconResourceRowKey() {
        return getVertex().getProperty(PropertyName.GLYPH_ICON.toString());
    }

    @Override
    public String getColor() {
        return getVertex().getProperty(PropertyName.COLOR.toString());
    }

    @Override
    public String getDisplayName() {
        return getVertex().getProperty(PropertyName.DISPLAY_NAME.toString());
    }

    public Vertex getVertex() {
        return this.vertex;
    }
}
