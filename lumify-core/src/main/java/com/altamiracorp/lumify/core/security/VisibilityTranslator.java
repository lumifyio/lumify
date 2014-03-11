package com.altamiracorp.lumify.core.security;

import org.json.JSONObject;

import java.util.Map;

public interface VisibilityTranslator {
    public static final String JSON_SOURCE = "source";
    public static final String JSON_WORKSPACES = "workspaces";

    void init(Map configuration);

    LumifyVisibility toVisibility(JSONObject visibilityJson);
}
