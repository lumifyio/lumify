package io.lumify.core.model;

import org.json.JSONObject;

import java.io.Serializable;

public class PropertyJustificationMetadata implements Serializable {
    private static final long serialVersionUID = 1L;
    public final String justificationText;

    public PropertyJustificationMetadata(String justificationText) {
        this.justificationText = justificationText;
    }

    public PropertyJustificationMetadata(JSONObject json) {
        this.justificationText = json.getString("justificationText");
    }

    public String getJustificationText() {
        return justificationText;
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("justificationText", getJustificationText());
        return jsonObject;
    }
}
