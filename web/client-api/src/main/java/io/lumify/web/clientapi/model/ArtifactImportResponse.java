package io.lumify.web.clientapi.model;

import org.json.JSONArray;
import org.json.JSONObject;

public class ArtifactImportResponse {
    private final JSONObject response;

    public ArtifactImportResponse(JSONObject response) {
        this.response = response;
    }

    public String[] getVertexIds() {
        JSONArray vertexIds = this.response.getJSONArray("vertexIds");
        String[] results = new String[vertexIds.length()];
        for (int i = 0; i < vertexIds.length(); i++) {
            results[i] = vertexIds.getString(i);
        }
        return results;
    }

    @Override
    public String toString() {
        return "ArtifactImportResponse{" +
                "response=" + response +
                '}';
    }
}
