package io.lumify.core.model.workspace.diff;

import io.lumify.core.model.properties.LumifyProperties;
import org.securegraph.Vertex;
import org.json.JSONObject;

public class VertexDiffItem extends DiffItem {
    private final Vertex vertex;
    private final boolean visible;

    public VertexDiffItem(Vertex vertex, SandboxStatus sandboxStatus, boolean visible) {
        super(VertexDiffItem.class.getSimpleName(), sandboxStatus);
        this.vertex = vertex;
        this.visible = visible;
    }

    @Override
    public JSONObject toJson() {
        String title = LumifyProperties.TITLE.getPropertyValue(vertex);

        JSONObject json = super.toJson();
        json.put("vertexId", vertex.getId());
        json.put("title", title);
        json.put("visible", visible);
        return json;
    }
}
