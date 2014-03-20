package com.altamiracorp.lumify.core.model.ontology;

public enum LabelName {
    HAS_PROPERTY("http://lumify.io/ontology#hasProperty"),
    HAS_EDGE("http://lumify.io/ontology#hasEdge"),
    IS_A("http://lumify.io/ontology#isA"),
    ENTITY_HAS_IMAGE_RAW("http://lumify.io/dev#entityHasImageRaw"),
    RAW_HAS_ENTITY("http://lumify.io/dev#rawHasEntity"),
    RAW_CONTAINS_IMAGE_OF_ENTITY("http://lumify.io/dev#rawContainsImageOfEntity");

    private final String text;

    LabelName(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return this.text;
    }
}
