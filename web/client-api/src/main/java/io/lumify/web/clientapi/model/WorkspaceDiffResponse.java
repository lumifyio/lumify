package io.lumify.web.clientapi.model;

import org.json.JSONArray;
import org.json.JSONObject;

public class WorkspaceDiffResponse {
    private final JSONObject response;

    public WorkspaceDiffResponse(JSONObject response) {
        this.response = response;
    }

    public WorkspaceDiffItem[] getDiffs() {
        JSONArray diffs = this.response.getJSONArray("diffs");
        WorkspaceDiffItem[] results = new WorkspaceDiffItem[diffs.length()];
        for (int i = 0; i < diffs.length(); i++) {
            results[i] = WorkspaceDiffItem.create(diffs.getJSONObject(i));
        }
        return results;
    }

    @Override
    public String toString() {
        return "WorkspaceDiffResponse{" +
                "response=" + response +
                '}';
    }
}
