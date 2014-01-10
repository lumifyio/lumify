package com.altamiracorp.lumify.core.model.ontology;

import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.tinkerpop.blueprints.Vertex;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GraphVertexConcept extends Concept {
    private final GraphVertex graphVertex;
    private final Map<String, Object> oldProperties;

    public GraphVertexConcept(GraphVertex graphVertex) {
        this.graphVertex = graphVertex;
        oldProperties = graphVertex.getOldProperties();
    }

    @Override
    public String getId() {
        return graphVertex.getId();
    }

    @Override
    public GraphVertex setProperty(String key, Object value) {
        if (graphVertex.getPropertyKeys().contains(key)) {
            oldProperties.put(key, graphVertex.getProperty(key));
        }
        graphVertex.setProperty(key, value);
        return this;
    }

    @Override
    public GraphVertex removeProperty(String key) {
        graphVertex.removeProperty(key);
        return this;
    }

    @Override
    public Set<String> getPropertyKeys() {
        return graphVertex.getPropertyKeys();
    }

    @Override
    public Object getProperty(String propertyKey) {
        return graphVertex.getProperty(propertyKey);
    }

    @Override
    public String getTitle() {
        return (String) graphVertex.getProperty(PropertyName.ONTOLOGY_TITLE);
    }

    @Override
    public String getGlyphIconResourceRowKey() {
        return (String) graphVertex.getProperty(PropertyName.GLYPH_ICON);
    }

    @Override
    public String getColor() {
        return (String) graphVertex.getProperty(PropertyName.COLOR);
    }

    @Override
    public String getDisplayName() {
        return (String) graphVertex.getProperty(PropertyName.DISPLAY_NAME);
    }

    @Override
    public String getDisplayType() {
        return (String) graphVertex.getProperty(PropertyName.DISPLAY_TYPE);
    }

    @Override
    public Vertex getVertex() {
        return graphVertex.getVertex();
    }

    @Override
    public Map<String, Object> getOldProperties () {
        return oldProperties;
    }

    public GraphVertex getGraphVertex() {
        return graphVertex;
    }
}
