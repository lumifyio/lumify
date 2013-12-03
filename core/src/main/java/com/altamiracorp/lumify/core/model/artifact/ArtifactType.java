package com.altamiracorp.lumify.core.model.artifact;

public enum ArtifactType {
    DOCUMENT("document"),
    IMAGE("image"),
    VIDEO("video");

    private final String text;

    ArtifactType(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return this.text;
    }

    public static ArtifactType convert(String property) {
        for (ArtifactType at : ArtifactType.values()) {
            if (at.toString().equalsIgnoreCase(property)) {
                return at;
            }
        }
        return ArtifactType.valueOf(property);
    }
}
