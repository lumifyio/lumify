package com.altamiracorp.lumify.core.model.ontology;

public enum VertexType {
    CONCEPT("concept"),
    ENTITY("entity"),
    PROPERTY("property"),
    RELATIONSHIP("relationship");

    private final String text;

    VertexType(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return this.text;
    }
}
