package com.altamiracorp.lumify.core.model.ontology;

import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.tinkerpop.blueprints.Vertex;

import java.util.HashMap;
import java.util.Set;

public class VertexConcept extends Concept {
    private final Vertex vertex;
    private HashMap<String, Object> oldProperties = new HashMap<String, Object>();

    public VertexConcept(Vertex vertex) {
        this.vertex = vertex;
    }

    public String getId() {
        return getVertex().getId().toString();
    }

    @Override
    public GraphVertex setProperty(String key, Object value) {
        if (this.vertex.getPropertyKeys().contains(key)) {
            oldProperties.put(key, this.vertex.getProperty(key));
        }
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

    @Override
    public String getDisplayType() {
        return getVertex().getProperty(PropertyName.DISPLAY_TYPE.toString());
    }

    @Override
    public HashMap<String, Object> getOldProperties () {
        return this.oldProperties;
    }

    public Vertex getVertex() {
        return this.vertex;
    }
}
