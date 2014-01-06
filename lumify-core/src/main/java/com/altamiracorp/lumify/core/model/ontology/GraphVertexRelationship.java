package com.altamiracorp.lumify.core.model.ontology;

import com.altamiracorp.lumify.core.model.graph.GraphVertex;

import java.util.Map;
import java.util.Set;

public class GraphVertexRelationship extends Relationship {
    private final GraphVertex vertex;
    private final Concept sourceConcept;
    private final Concept destConcept;
    private final Map<String, Object> oldProperties;

    public GraphVertexRelationship(GraphVertex vertex, Concept sourceConcept, Concept destConcept) {
        this.vertex = vertex;
        this.sourceConcept = sourceConcept;
        this.destConcept = destConcept;
        oldProperties = vertex.getOldProperties();
    }

    @Override
    public String getId() {
        return vertex.getId();
    }

    @Override
    public GraphVertex setProperty(String key, Object value) {
        if (vertex.getPropertyKeys().contains(key)) {
            oldProperties.put(key, vertex.getProperty(key));
        }
        vertex.setProperty(key, value);
        return this;
    }

    @Override
    public GraphVertex removeProperty(String key) {
        vertex.removeProperty(key);
        return this;
    }

    @Override
    public Set<String> getPropertyKeys() {
        return vertex.getPropertyKeys();
    }

    @Override
    public Object getProperty(String propertyKey) {
        return vertex.getProperty(propertyKey);
    }

    @Override
    public String getTitle() {
        return (String) vertex.getProperty(PropertyName.ONTOLOGY_TITLE);
    }

    @Override
    public String getDisplayName() {
        return (String) vertex.getProperty(PropertyName.DISPLAY_NAME);
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
    public Map<String, Object> getOldProperties () {
        return oldProperties;
    }
}
