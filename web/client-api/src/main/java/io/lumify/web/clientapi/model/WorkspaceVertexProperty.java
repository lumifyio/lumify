package io.lumify.web.clientapi.model;

import org.json.JSONObject;

public class WorkspaceVertexProperty {
    private final JSONObject json;

    public WorkspaceVertexProperty(JSONObject json) {
        this.json = json;
    }

    @Override
    public String toString() {
        return "WorkspaceVertexProperty{" +
                "json=" + json +
                '}';
    }

    public String getName() {
        return json.getString("name");
    }

    public String getKey() {
        return json.getString("key");
    }

    public Object getValue() {
        return json.opt("value");
    }

    public boolean isStreamingPropertyValue() {
        return json.optBoolean("streamingPropertyValue", false);
    }
}
