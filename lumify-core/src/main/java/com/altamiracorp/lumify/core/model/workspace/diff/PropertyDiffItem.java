package com.altamiracorp.lumify.core.model.workspace.diff;

import com.altamiracorp.lumify.core.model.properties.LumifyProperties;
import com.altamiracorp.lumify.core.util.GraphUtil;
import com.altamiracorp.securegraph.Property;
import com.altamiracorp.securegraph.Vertex;
import org.json.JSONObject;

public class PropertyDiffItem extends DiffItem {
    private final Vertex vertex;
    private final Property workspaceProperty;
    private final Property existingProperty;

    public PropertyDiffItem(Vertex vertex, Property workspaceProperty, Property existingProperty) {
        super(PropertyDiffItem.class.getSimpleName(), getMessage(vertex, workspaceProperty));
        this.vertex = vertex;
        this.workspaceProperty = workspaceProperty;
        this.existingProperty = existingProperty;
    }

    private static String getMessage(Vertex vertex, Property property) {
        String vertexTitle = LumifyProperties.TITLE.getPropertyValue(vertex);
        return "Property " + property.getName() + " changed on " + vertexTitle;
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = super.toJson();
        json.put("vertexId", vertex.getId());
        json.put("name", workspaceProperty.getName());
        json.put("key", workspaceProperty.getKey());
        if (existingProperty != null) {
            json.put("old", GraphUtil.toJsonProperty(existingProperty));
        }
        json.put("new", GraphUtil.toJsonProperty(workspaceProperty));
        return json;
    }
}
