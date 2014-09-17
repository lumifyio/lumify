package io.lumify.web.clientapi.model;

import org.json.JSONArray;
import org.json.JSONObject;

public class WorkspacePublishResponse {
    private final JSONObject response;

    public WorkspacePublishResponse(JSONObject response) {
        this.response = response;
    }

    @Override
    public String toString() {
        return "WorkspacePublishResponse{" +
                "response=" + response +
                '}';
    }

    public PublishFailure[] getFailures() {
        JSONArray failures = this.response.getJSONArray("failures");
        PublishFailure[] results = new PublishFailure[failures.length()];
        for (int i = 0; i < failures.length(); i++) {
            results[i] = new PublishFailure(failures.getJSONObject(i));
        }
        return results;
    }
}
