package com.altamiracorp.lumify.core.model.ontology;

import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.tinkerpop.blueprints.Vertex;

import java.util.HashMap;
import java.util.Set;

public class VertexRelationship extends Relationship {
    private final Vertex vertex;
    private final Concept sourceConcept;
    private final Concept destConcept;
    private HashMap<String, Object> oldProperties = new HashMap<String, Object>();

    public VertexRelationship(Vertex vertex, Concept sourceConcept, Concept destConcept) {
        this.vertex = vertex;
        this.sourceConcept = sourceConcept;
        this.destConcept = destConcept;
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
    public Concept getSourceConcept() {
        return sourceConcept;
    }

    @Override
    public Concept getDestConcept() {
        return destConcept;
    }

    @Override
    public HashMap<String, Object> getOldProperties () {
        return this.getOldProperties();
    }

    public Vertex getVertex() {
        return vertex;
    }
}
