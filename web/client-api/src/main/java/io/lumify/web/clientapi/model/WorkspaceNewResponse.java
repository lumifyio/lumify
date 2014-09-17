package io.lumify.web.clientapi.model;

import org.json.JSONObject;

public class WorkspaceNewResponse {
    private final JSONObject responseJson;

    public WorkspaceNewResponse(JSONObject responseJson) {
        this.responseJson = responseJson;
    }

    public Workspace getWorkspace() {
        return new Workspace(this.responseJson);
    }

    @Override
    public String toString() {
        return "WorkspaceNewResponse{" +
                "responseJson=" + responseJson +
                '}';
    }
}
