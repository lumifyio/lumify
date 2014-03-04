package com.altamiracorp.lumify.core.security;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DirectVisibilityTranslator implements VisibilityTranslator {
    public void init(Map configuration) {

    }

    @Override
    public LumifyVisibility toVisibility(JSONObject visibilityJson) {
        StringBuilder visibilityString = new StringBuilder();

        List<String> all = new ArrayList<String>();

        String source = visibilityJson.optString("source");
        if (source != null && source.trim().length() > 0) {
            all.add(source.trim());
        }

        JSONArray workspaces = visibilityJson.optJSONArray("workspaces");
        if (workspaces != null) {
            for (int i = 0; i < workspaces.length(); i++) {
                String workspace = workspaces.getString(i);
                all.add(workspace);
            }
        }

        for (int i = 0; i < all.size(); i++) {
            String additionalRequiredVisibility = all.get(i);
            if (i > 0) {
                visibilityString.append("&");
            }
            visibilityString
                    .append("(")
                    .append(additionalRequiredVisibility)
                    .append(")");
        }

        return new LumifyVisibility(visibilityString.toString());
    }
}
