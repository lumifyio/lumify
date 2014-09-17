package io.lumify.web.clientapi.model;

import org.json.JSONObject;

public class EdgePublishItem extends PublishItem {
    private final String edgeId;
    private final String sourceVertexId;
    private final String destVertexId;

    public EdgePublishItem(String edgeId, String sourceVertexId, String destVertexId) {
        this.edgeId = edgeId;
        this.sourceVertexId = sourceVertexId;
        this.destVertexId = destVertexId;
    }

    @Override
    public JSONObject getJson() {
        JSONObject json = new JSONObject();
        json.put("type", "edge");
        json.put("action", "");
        json.put("edgeId", edgeId);
        json.put("sourceId", sourceVertexId);
        json.put("destId", destVertexId);
        return json;
    }
}
