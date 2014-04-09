package com.altamiracorp.lumify.core.security;

import com.altamiracorp.lumify.core.model.properties.JsonLumifyProperty;
import com.altamiracorp.lumify.core.model.properties.TextLumifyProperty;

// TODO convert to property types. See com.altamiracorp.lumify.core.model.properties.LumifyProperties
public class LumifyVisibilityProperties {
    public static final JsonLumifyProperty VISIBILITY_JSON_PROPERTY = new JsonLumifyProperty("http://lumify.io#visibilityJson");
    public static final TextLumifyProperty VISIBILITY_PROPERTY = TextLumifyProperty.all("http://lumify.io#visibility");

    private LumifyVisibilityProperties() {
        throw new UnsupportedOperationException("do not construct utility class");
    }
}
