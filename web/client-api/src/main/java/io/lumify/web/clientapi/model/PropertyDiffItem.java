package io.lumify.web.clientapi.model;

import org.json.JSONObject;

public class PropertyDiffItem extends WorkspaceDiffItem {
    public PropertyDiffItem(JSONObject diffJson) {
        super(diffJson);
    }

    public String getElementId() {
        return getDiffJson().getString("elementId");
    }

    public String getPropertyKey() {
        return getDiffJson().getString("key");
    }

    public String getPropertyName() {
        return getDiffJson().getString("name");
    }
}
