package io.lumify.web.clientapi.model;

import org.json.JSONObject;

public class VertexWorkspaceUpdateItem extends WorkspaceUpdateItem {
    private final String vertexId;
    private final GraphPosition graphPosition;

    public VertexWorkspaceUpdateItem(String vertexId, GraphPosition graphPosition) {
        this.vertexId = vertexId;
        this.graphPosition = graphPosition;
    }

    @Override
    public JSONObject getJson() {
        JSONObject json = new JSONObject();
        json.put("vertexId", vertexId);
        json.put("graphPosition", graphPosition.toJson());
        return json;
    }
}
