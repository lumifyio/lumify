package io.lumify.core.model.workspace.diff;

import io.lumify.core.util.JsonSerializer;
import org.securegraph.Element;
import org.securegraph.Property;
import org.json.JSONObject;

public class PropertyDiffItem extends DiffItem {
    private final Element element;
    private final Property workspaceProperty;
    private final Property existingProperty;

    public PropertyDiffItem(Element element, Property workspaceProperty, Property existingProperty, SandboxStatus sandboxStatus) {
        super(PropertyDiffItem.class.getSimpleName(), sandboxStatus);
        this.element = element;
        this.workspaceProperty = workspaceProperty;
        this.existingProperty = existingProperty;
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = super.toJson();
        json.put("elementId", element.getId());
        json.put("name", workspaceProperty.getName());
        json.put("key", workspaceProperty.getKey());
        if (existingProperty != null) {
            json.put("old", JsonSerializer.toJsonProperty(existingProperty));
        }
        json.put("new", JsonSerializer.toJsonProperty(workspaceProperty));
        return json;
    }
}
