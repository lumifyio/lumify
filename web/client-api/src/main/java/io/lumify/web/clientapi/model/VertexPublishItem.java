package io.lumify.web.clientapi.model;

import org.json.JSONObject;

public class VertexPublishItem extends PublishItem {
    private final String vertexId;

    public VertexPublishItem(String vertexId) {
        this.vertexId = vertexId;
    }

    @Override
    public JSONObject getJson() {
        JSONObject json = new JSONObject();
        json.put("type", "vertex");
        json.put("action", "");
        json.put("vertexId", vertexId);
        return json;
    }
}
