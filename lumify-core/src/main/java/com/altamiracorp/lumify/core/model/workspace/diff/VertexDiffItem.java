package com.altamiracorp.lumify.core.model.workspace.diff;

import com.altamiracorp.lumify.core.model.properties.LumifyProperties;
import com.altamiracorp.securegraph.Vertex;
import org.json.JSONObject;

public class VertexDiffItem extends DiffItem {
    private final Vertex vertex;

    public VertexDiffItem(Vertex vertex, SandboxStatus sandboxStatus) {
        super(VertexDiffItem.class.getSimpleName(), sandboxStatus);
        this.vertex = vertex;
    }

    @Override
    public JSONObject toJson() {
        String title = LumifyProperties.TITLE.getPropertyValue(vertex);

        JSONObject json = super.toJson();
        json.put("vertexId", vertex.getId());
        json.put("title", title);
        return json;
    }
}
