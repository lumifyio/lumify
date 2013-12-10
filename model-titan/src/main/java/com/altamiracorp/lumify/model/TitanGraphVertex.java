package com.altamiracorp.lumify.model;

import com.altamiracorp.lumify.core.model.graph.GraphGeoLocation;
import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.thinkaurelius.titan.core.TitanVertex;
import com.thinkaurelius.titan.core.attribute.Geoshape;
import com.tinkerpop.blueprints.Vertex;

import java.util.HashMap;
import java.util.Set;

public class TitanGraphVertex extends GraphVertex {
    private final Vertex vertex;
    private HashMap<String, Object> oldProperties = new HashMap<String, Object>();

    public TitanGraphVertex(Vertex vertex) {
        this.vertex = vertex;
    }

    @Override
    public String getId() {
        return "" + this.vertex.getId();
    }

    @Override
    public GraphVertex setProperty(String key, Object value) {
        if (this.vertex.getPropertyKeys().contains(key)) {
            oldProperties.put(key, this.vertex.getProperty(key));
        }
        if (value instanceof GraphGeoLocation) {
            GraphGeoLocation loc = (GraphGeoLocation) value;
            value = Geoshape.point(loc.getLatitude(), loc.getLongitude());
        }
        if (!value.equals(getProperty(key))) {
            this.vertex.setProperty(key, value);
        }
        return this;
    }

    @Override
    public GraphVertex removeProperty(String key) {
        this.vertex.removeProperty(key);
        return this;
    }

    public Set<String> getPropertyKeys() {
        return this.vertex.getPropertyKeys();
    }

    @Override
    public Object getProperty(String propertyKey) {
        return this.vertex.getProperty(propertyKey);
    }

    @Override
    public HashMap<String, Object> getOldProperties () {
        return this.oldProperties;
    }

    public Vertex getVertex() {
        return vertex;
    }

    public void addProperty(String key, Object attribute) {
        ((TitanVertex) this.vertex).addProperty(key, attribute);
    }
}
