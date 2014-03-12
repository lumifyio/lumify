package com.altamiracorp.lumify.core.model.workspace.diff;

import com.altamiracorp.lumify.core.util.GraphUtil;
import com.altamiracorp.securegraph.Element;
import com.altamiracorp.securegraph.Property;
import org.json.JSONObject;

public class PropertyDiffItem extends DiffItem {
    private final Element element;
    private final Property workspaceProperty;
    private final Property existingProperty;

    public PropertyDiffItem(Element element, Property workspaceProperty, Property existingProperty, SandboxStatus sandboxStatus) {
        super(PropertyDiffItem.class.getSimpleName(), getMessage(element, workspaceProperty), sandboxStatus);
        this.element = element;
        this.workspaceProperty = workspaceProperty;
        this.existingProperty = existingProperty;
    }

    private static String getMessage(Element element, Property property) {
        return "Property " + property.getName() + " changed on " + element.getId();
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = super.toJson();
        json.put("elementId", element.getId());
        json.put("name", workspaceProperty.getName());
        json.put("key", workspaceProperty.getKey());
        if (existingProperty != null) {
            json.put("old", GraphUtil.toJsonProperty(existingProperty));
        }
        json.put("new", GraphUtil.toJsonProperty(workspaceProperty));
        return json;
    }
}
