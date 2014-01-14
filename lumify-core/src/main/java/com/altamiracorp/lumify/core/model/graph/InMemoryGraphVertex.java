package com.altamiracorp.lumify.core.model.graph;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class InMemoryGraphVertex extends GraphVertex {
    private String id;
    private final Map<String, Object> newProperties = new HashMap<String, Object>();
    private final Map<String, Object> oldProperties = new HashMap<String, Object>();

    public InMemoryGraphVertex() {
        this.id = null;
    }

    public InMemoryGraphVertex(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public GraphVertex setProperty(String key, Object value) {
        newProperties.put(key, value);
        return this;
    }

    @Override
    public GraphVertex removeProperty(String key) {
        if (newProperties.containsKey(key)) {
            newProperties.remove(key);
        }
        return this;
    }

    @Override
    public Set<String> getPropertyKeys() {
        return Collections.unmodifiableSet(newProperties.keySet());
    }

    @Override
    public Object getProperty(String propertyKey) {
        return newProperties.get(propertyKey);
    }

    @Override
    public Map<String, Object> getOldProperties () {
        return Collections.unmodifiableMap(oldProperties);
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public void update(GraphVertex newGraphVertex) {
        super.update(newGraphVertex);
        if (newGraphVertex.getId() != null) {
            setId(newGraphVertex.getId());
        }
    }
}
