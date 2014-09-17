package io.lumify.web.clientapi.model;

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

    @Override
    public String toString() {
        return "WorkspaceVertex{" +
                "json=" + json +
                '}';
    }
}
