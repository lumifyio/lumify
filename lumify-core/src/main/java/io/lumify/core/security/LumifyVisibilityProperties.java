package io.lumify.core.security;

import io.lumify.core.model.properties.types.JsonLumifyProperty;
import io.lumify.core.model.properties.types.TextLumifyProperty;

// TODO convert to property types. See io.lumify.core.model.properties.LumifyProperties
public class LumifyVisibilityProperties {
    public static final JsonLumifyProperty VISIBILITY_JSON_PROPERTY = new JsonLumifyProperty("http://lumify.io#visibilityJson");
    public static final TextLumifyProperty VISIBILITY_PROPERTY = TextLumifyProperty.all("http://lumify.io#visibility");

    private LumifyVisibilityProperties() {
        throw new UnsupportedOperationException("do not construct utility class");
    }
}
