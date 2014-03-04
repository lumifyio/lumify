package com.altamiracorp.lumify.core.model.workspace.diff;

import com.altamiracorp.lumify.core.model.properties.LumifyProperties;
import com.altamiracorp.securegraph.Vertex;
import org.json.JSONObject;

public class VertexDiffItem extends DiffItem {
    private final Vertex vertex;

    public VertexDiffItem(Vertex vertex) {
        super(VertexDiffItem.class.getSimpleName(), getMessage(vertex));
        this.vertex = vertex;
    }

    private static String getMessage(Vertex vertex) {
        String title = LumifyProperties.TITLE.getPropertyValue(vertex);
        return "Entity " + title + " added";
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = super.toJson();
        json.put("vertexId", vertex.getId());
        return json;
    }
}
