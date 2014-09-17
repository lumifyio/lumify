package io.lumify.web.clientapi.model;

import org.json.JSONObject;

public class WorkspaceUpdateResponse {
    private final JSONObject response;

    public WorkspaceUpdateResponse(JSONObject response) {
        this.response = response;
    }

    @Override
    public String toString() {
        return "WorkspaceUpdateResponse{" +
                "response=" + response +
                '}';
    }
}
