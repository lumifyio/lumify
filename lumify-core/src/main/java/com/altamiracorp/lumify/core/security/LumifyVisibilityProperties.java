package com.altamiracorp.lumify.core.security;

public enum LumifyVisibilityProperties {
    VISIBILITY_PROPERTY("_visibility"),
    VISIBILITY_JSON_PROPERTY("_visibilityJson");

    private final String property;

    LumifyVisibilityProperties (String property) {
        this.property = property;
    }

    @Override
    public final String toString () {
        return this.property;
    }
}
