package com.altamiracorp.lumify.core.model.workspace.diff;

import com.altamiracorp.securegraph.Direction;
import com.altamiracorp.securegraph.Edge;
import org.json.JSONObject;

public class EdgeDiffItem extends DiffItem {
    private final Edge edge;

    public EdgeDiffItem(Edge edge) {
        super(EdgeDiffItem.class.getSimpleName(), getMessage(edge));
        this.edge = edge;
    }

    private static String getMessage(Edge edge) {
        return "Edge added";
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = super.toJson();
        json.put("edgeId", edge.getId());
        json.put("outVertexId", edge.getVertexId(Direction.OUT));
        json.put("inVertexId", edge.getVertexId(Direction.IN));
        return json;
    }
}
