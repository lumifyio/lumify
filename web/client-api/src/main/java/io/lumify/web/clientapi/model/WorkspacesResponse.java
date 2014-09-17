package io.lumify.web.clientapi.model;

import org.json.JSONArray;
import org.json.JSONObject;

public class WorkspacesResponse {
    private final JSONObject responseJson;

    public WorkspacesResponse(JSONObject responseJson) {
        this.responseJson = responseJson;
    }

    public Workspace[] getWorkspaces() {
        JSONArray workspaces = this.responseJson.getJSONArray("workspaces");
        Workspace[] results = new Workspace[workspaces.length()];
        for (int i = 0; i < workspaces.length(); i++) {
            results[i] = new Workspace(workspaces.getJSONObject(i));
        }
        return results;
    }

    @Override
    public String toString() {
        return "WorkspacesResponse{" +
                "responseJson=" + responseJson +
                '}';
    }
}
