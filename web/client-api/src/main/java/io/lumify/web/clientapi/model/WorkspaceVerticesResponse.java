package io.lumify.web.clientapi.model;

import org.json.JSONArray;

public class WorkspaceVerticesResponse {
    private final JSONArray response;

    public WorkspaceVerticesResponse(JSONArray response) {
        this.response = response;
    }

    @Override
    public String toString() {
        return "WorkspaceVerticesResponse{" +
                "response=" + response +
                '}';
    }

    public WorkspaceVertex[] getVertices() {
        WorkspaceVertex[] results = new WorkspaceVertex[this.response.length()];
        for (int i = 0; i < this.response.length(); i++) {
            results[i] = new WorkspaceVertex(this.response.getJSONObject(i));
        }
        return results;
    }
}
