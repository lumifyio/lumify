package com.altamiracorp.lumify.model;

import com.altamiracorp.lumify.core.model.graph.GraphRelationship;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;

import java.util.HashMap;

public class TitanGraphRelationship extends GraphRelationship {

    public TitanGraphRelationship(Edge e) {
        super(e.getId().toString(), e.getVertex(Direction.OUT).getId().toString(), e.getVertex(Direction.IN).getId().toString(), e.getLabel());
    }

    public HashMap<String, Object> getAllProperty(Edge e) {
        HashMap<String, Object> properties = new HashMap<String, Object>();
        for (String propertyKey : e.getPropertyKeys()) {
            properties.put(propertyKey, e.getProperty(propertyKey));
        }
        return properties;
    }
}
