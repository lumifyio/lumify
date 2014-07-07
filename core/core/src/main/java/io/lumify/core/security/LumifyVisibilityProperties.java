package io.lumify.core.security;

import io.lumify.core.model.properties.types.JsonLumifyProperty;
import io.lumify.core.model.properties.types.StringLumifyProperty;

public class LumifyVisibilityProperties {
    public static final JsonLumifyProperty VISIBILITY_JSON_PROPERTY = new JsonLumifyProperty("http://lumify.io#visibilityJson");
    public static final StringLumifyProperty VISIBILITY_PROPERTY = new StringLumifyProperty("http://lumify.io#visibility");

    private LumifyVisibilityProperties() {
        throw new UnsupportedOperationException("do not construct utility class");
    }
}
