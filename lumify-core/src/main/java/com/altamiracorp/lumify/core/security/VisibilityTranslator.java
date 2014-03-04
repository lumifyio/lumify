package com.altamiracorp.lumify.core.security;

import com.altamiracorp.securegraph.Visibility;
import org.json.JSONObject;

import java.util.Map;

public interface VisibilityTranslator {
    void init(Map configuration);

    Visibility toVisibility(JSONObject visibilityJson);
}
