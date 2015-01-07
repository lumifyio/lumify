package io.lumify.web.clientapi.model;

public class ClientApiRelationshipPublishItem extends ClientApiPublishItem {
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
}
