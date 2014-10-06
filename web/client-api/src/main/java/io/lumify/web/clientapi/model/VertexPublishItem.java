package io.lumify.web.clientapi.model;

public class VertexPublishItem extends PublishItem {
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

    @Override
    public String toString() {
        return "VertexPublishItem{" +
                super.toString() +
                ", vertexId='" + vertexId + '\'' +
                '}';
    }
}
