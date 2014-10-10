package io.lumify.web.clientapi.model;

public class ClientApiVertexPublishItem extends ClientApiPublishItem {
    private String vertexId;

    public String getVertexId() {
        return vertexId;
    }

    public void setVertexId(String vertexId) {
        this.vertexId = vertexId;
    }

    @Override
    public String getType() {
        return "vertex";
    }
}
