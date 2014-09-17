package io.lumify.web.clientapi.model;

import org.json.JSONArray;
import org.json.JSONObject;

public class Workspace {
    private final JSONObject workspaceJson;

    public Workspace(JSONObject workspaceJson) {
        this.workspaceJson = workspaceJson;
    }

    public String getId() {
        return this.workspaceJson.getString("workspaceId");
    }

    public String getTitle() {
        return this.workspaceJson.getString("title");
    }

    public boolean isSharedToUser() {
        return this.workspaceJson.getBoolean("isSharedToUser");
    }

    public boolean isEditable() {
        return this.workspaceJson.getBoolean("isEditable");
    }

    public String getCreatedByUserId() {
        return this.workspaceJson.getString("createdBy");
    }

    public WorkspaceUser[] getUsers() {
        JSONArray users = this.workspaceJson.getJSONArray("users");
        WorkspaceUser[] results = new WorkspaceUser[users.length()];
        for (int i = 0; i < users.length(); i++) {
            results[i] = new WorkspaceUser(users.getJSONObject(i));
        }
        return results;
    }

    @Override
    public String toString() {
        return "Workspace{" +
                "workspaceJson=" + workspaceJson +
                '}';
    }
}