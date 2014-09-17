package io.lumify.web.clientapi.model;

import org.json.JSONObject;

public class EdgeDiffItem extends WorkspaceDiffItem {
    public EdgeDiffItem(JSONObject diffJson) {
        super(diffJson);
    }

    public String getEdgeId() {
        return getDiffJson().getString("edgeId");
    }

    public String getSourceVertexId() {
        return getDiffJson().getString("outVertexId");
    }

    public String getDestVertexId() {
        return getDiffJson().getString("inVertexId");
    }
}
