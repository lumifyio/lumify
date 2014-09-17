package io.lumify.web.clientapi.model;

import org.json.JSONArray;
import org.json.JSONObject;

public class WorkspaceVertex {
    private final JSONObject json;

    public WorkspaceVertex(JSONObject json) {
        this.json = json;
    }

    public String getId() {
        return this.json.getString("id");
    }

    public SandboxStatus getSandboxStatus() {
        return SandboxStatus.valueOf(this.json.getString("sandboxStatus"));
    }

    public WorkspaceVertexProperty[] getProperties() {
        JSONArray properties = this.json.getJSONArray("properties");
        WorkspaceVertexProperty[] results = new WorkspaceVertexProperty[properties.length()];
        for (int i = 0; i < properties.length(); i++) {
            results[i] = new WorkspaceVertexProperty(properties.getJSONObject(i));
        }
        return results;
    }

    @Override
    public String toString() {
        return "WorkspaceVertex{" +
                "json=" + json +
                '}';
    }
}
