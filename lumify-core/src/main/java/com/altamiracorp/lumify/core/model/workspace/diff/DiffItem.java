package com.altamiracorp.lumify.core.model.workspace.diff;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public abstract class DiffItem {
    private final String type;
    private final SandboxStatus sandboxStatus;

    protected DiffItem(String type, SandboxStatus sandboxStatus) {
        this.type = type;
        this.sandboxStatus = sandboxStatus;
    }

    public static JSONArray toJson(List<DiffItem> diffItems) {
        JSONArray resultsJson = new JSONArray();
        for (DiffItem diffItem : diffItems) {
            resultsJson.put(diffItem.toJson());
        }
        return resultsJson;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("type", getType());
        json.put("sandboxStatus", sandboxStatus.toString());
        return json;
    }

    public String getType() {
        return type;
    }
}
