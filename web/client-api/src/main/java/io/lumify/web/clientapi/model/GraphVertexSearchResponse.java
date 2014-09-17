package io.lumify.web.clientapi.model;

import org.json.JSONArray;
import org.json.JSONObject;

public class GraphVertexSearchResponse {
    private final JSONObject json;

    public GraphVertexSearchResponse(JSONObject json) {
        this.json = json;
    }

    @Override
    public String toString() {
        return "GraphVertexSearchResponse{" +
                "json=" + json +
                '}';
    }

    public long getRetrievalTimeNano() {
        return json.optLong("retrievalTime");
    }

    public int getTotalHits() {
        return json.optInt("totalHits");
    }

    public GraphVertexSearchVertex[] getVertices() {
        JSONArray vertices = json.getJSONArray("vertices");
        GraphVertexSearchVertex[] results = new GraphVertexSearchVertex[vertices.length()];
        for (int i = 0; i < vertices.length(); i++) {
            results[i] = new GraphVertexSearchVertex(vertices.getJSONObject(i));
        }
        return results;
    }
}
