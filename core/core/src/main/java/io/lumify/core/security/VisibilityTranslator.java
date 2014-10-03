package io.lumify.core.security;

import org.json.JSONObject;

import java.util.Map;

public abstract class VisibilityTranslator {
    public static final String JSON_SOURCE = "source";
    public static final String JSON_WORKSPACES = "workspaces";

    public abstract void init(Map configuration);

    public abstract LumifyVisibility toVisibility(JSONObject visibilityJson);

    public static String getVisibilitySource(JSONObject visibilityJson) {
        return visibilityJson.getString(JSON_SOURCE);
    }
}
