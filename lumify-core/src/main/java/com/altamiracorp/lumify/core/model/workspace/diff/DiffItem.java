package com.altamiracorp.lumify.core.model.workspace.diff;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public abstract class DiffItem {
    private final String type;
    private final String message;
    private final SandboxStatus sandboxStatus;

    protected DiffItem(String type, String message, SandboxStatus sandboxStatus) {
        this.type = type;
        this.message = message;
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
        json.put("message", getMessage());
        json.put("sandboxStatus", sandboxStatus.toString());
        return json;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }
}
