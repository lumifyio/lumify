package com.altamiracorp.lumify.core.model.ontology;

import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.tinkerpop.blueprints.Vertex;

import java.util.HashMap;
import java.util.Set;

public class VertexProperty extends Property {
    private final Vertex vertex;
    private HashMap<String, Object> oldProperties = new HashMap<String, Object>();

    public VertexProperty(Vertex vertex) {
        this.vertex = vertex;
    }

    @Override
    public String getId() {
        return getVertex().getId().toString();
    }

    @Override
    public GraphVertex setProperty(String key, Object value) {
        if (getVertex().getPropertyKeys().contains(key)) {
            oldProperties.put(key, getVertex().getProperty(key));
        }
        getVertex().setProperty(key, value);
        return this;
    }

    @Override
    public GraphVertex removeProperty(String key) {
        vertex.removeProperty(key);
        return this;
    }

    @Override
    public Set<String> getPropertyKeys() {
        return getVertex().getPropertyKeys();
    }

    @Override
    public Object getProperty(String propertyKey) {
        return getVertex().getProperty(propertyKey);
    }

    @Override
    public String getTitle() {
        return getVertex().getProperty(PropertyName.ONTOLOGY_TITLE.toString());
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
    public PropertyType getDataType() {
        return PropertyType.convert((String) getVertex().getProperty(PropertyName.DATA_TYPE.toString()));
    }

    @Override
    public HashMap<String, Object> getOldProperties () {
        return this.oldProperties;
    }

    public Vertex getVertex() {
        return vertex;
    }
}
