package com.altamiracorp.lumify.core.model.ontology;

public enum DisplayType {
    IMAGE("image"),
    VIDEO("video"),
    AUDIO("audio"),
    DOCUMENT("document");

    private final String text;

    DisplayType(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return this.text;
    }
}
