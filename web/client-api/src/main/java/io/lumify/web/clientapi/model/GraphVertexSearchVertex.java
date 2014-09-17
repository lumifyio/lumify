package io.lumify.web.clientapi.model;

import org.json.JSONObject;

public class GraphVertexSearchVertex {
    private final JSONObject json;

    public GraphVertexSearchVertex(JSONObject json) {
        this.json = json;
    }

    @Override
    public String toString() {
        return "GraphVertexSearchVertex{" +
                "json=" + json +
                '}';
    }
}
