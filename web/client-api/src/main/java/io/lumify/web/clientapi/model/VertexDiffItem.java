package io.lumify.web.clientapi.model;

import org.json.JSONObject;

public class VertexDiffItem extends WorkspaceDiffItem {
    public VertexDiffItem(JSONObject diffJson) {
        super(diffJson);
    }

    public String getVertexId() {
        return getDiffJson().getString("vertexId");
    }
}
