package io.lumify.core.model.workspace.diff;

import io.lumify.core.security.LumifyVisibilityProperties;
import io.lumify.core.util.JsonSerializer;
import org.json.JSONObject;
import org.securegraph.Direction;
import org.securegraph.Edge;

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
        json.put("visibilityJson", JsonSerializer.toJsonProperty(LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.getProperty(edge)));
        return json;
    }
}
