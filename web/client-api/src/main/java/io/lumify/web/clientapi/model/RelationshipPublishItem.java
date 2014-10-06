package io.lumify.web.clientapi.model;

public class RelationshipPublishItem extends PublishItem {
    private String edgeId;

    public String getEdgeId() {
        return edgeId;
    }

    public void setEdgeId(String edgeId) {
        this.edgeId = edgeId;
    }

    @Override
    public String getType() {
        return "relationship";
    }

    @Override
    public String toString() {
        return "RelationshipPublishItem{" +
                super.toString() +
                ", edgeId='" + edgeId + '\'' +
                '}';
    }
}
