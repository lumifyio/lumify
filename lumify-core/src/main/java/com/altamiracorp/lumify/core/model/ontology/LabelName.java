package com.altamiracorp.lumify.core.model.ontology;

public enum LabelName {
    HAS_PROPERTY("hasProperty"),
    HAS_EDGE("hasEdge"),
    IS_A("isA"),
    ENTITY_HAS_IMAGE_RAW("entityHasImageRaw"),
    RAW_HAS_ENTITY("rawHasEntity"),
    RAW_CONTAINS_IMAGE_OF_ENTITY("rawContainsImageOfEntity");

    private final String text;

    LabelName(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return this.text;
    }
}
