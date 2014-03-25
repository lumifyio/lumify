package com.altamiracorp.lumify.core.model.ontology;

public enum ConceptType {
    IMAGE("http://lumify.io/dev#image"),
    AUDIO("http://lumify.io/dev#audio"),
    VIDEO("http://lumify.io/dev#video"),
    DOCUMENT("http://lumify.io/dev#document");

    private final String text;

    ConceptType(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return this.text;
    }
}
