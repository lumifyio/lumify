package io.lumify.web.clientapi.model;

import org.json.JSONObject;

public class WorkspaceUser {
    private final JSONObject workspaceUserJson;

    public WorkspaceUser(JSONObject workspaceUserJson) {
        this.workspaceUserJson = workspaceUserJson;
    }

    public String getUserId() {
        return this.workspaceUserJson.getString("userId");
    }

    public String getAccess() {
        return this.workspaceUserJson.getString("access");
    }

    @Override
    public String toString() {
        return "WorkspaceUser{" +
                "workspaceUserJson=" + workspaceUserJson +
                '}';
    }
}
