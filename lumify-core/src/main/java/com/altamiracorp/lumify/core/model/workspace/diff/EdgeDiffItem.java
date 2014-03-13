package com.altamiracorp.lumify.core.model.workspace.diff;

import com.altamiracorp.securegraph.Direction;
import com.altamiracorp.securegraph.Edge;
import org.json.JSONObject;

public class EdgeDiffItem extends DiffItem {
    private final Edge edge;

    public EdgeDiffItem(Edge edge, SandboxStatus sandboxStatus) {
        super(EdgeDiffItem.class.getSimpleName(), sandboxStatus);
        this.edge = edge;
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = super.toJson();
        json.put("edgeId", edge.getId());
        json.put("label", edge.getLabel());
        json.put("outVertexId", edge.getVertexId(Direction.OUT));
        json.put("inVertexId", edge.getVertexId(Direction.IN));
        return json;
    }
}
