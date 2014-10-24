package io.lumify.web.clientapi.model;

public class ClientApiPropertyUndoItem extends ClientApiUndoItem {
    private String key;
    private String name;
    private String edgeId;
    private String vertexId;
    private String elementId;
    private String visibilityString;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEdgeId() {
        return edgeId;
    }

    public void setEdgeId(String edgeId) {
        this.edgeId = edgeId;
    }

    public String getVertexId() {
        return vertexId;
    }

    public void setVertexId(String vertexId) {
        this.vertexId = vertexId;
    }

    public String getElementId() {
        return elementId;
    }

    public void setElementId(String elementId) {
        this.elementId = elementId;
    }

    public String getVisibilityString() {
        return visibilityString;
    }

    public void setVisibilityString(String visibilityString) {
        this.visibilityString = visibilityString;
    }

    @Override
    public String getType() {
        return "property";
    }
}
