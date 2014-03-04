package com.altamiracorp.lumify.core.security;

import org.json.JSONObject;

import java.util.Map;

public interface VisibilityTranslator {
    void init(Map configuration);

    LumifyVisibility toVisibility(JSONObject visibilityJson);
}
