package com.altamiracorp.lumify.core.model.ontology;

public enum LabelName {
    HAS_PROPERTY("hasProperty"),
    HAS_EDGE("hasEdge"),
    IS_A("isA"),
    HAS_IMAGE("hasImage"),
    HAS_ENTITY("hasEntity"),
    CONTAINS_IMAGE_OF("containsImageOf");

    private final String text;

    LabelName(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return this.text;
    }
}
