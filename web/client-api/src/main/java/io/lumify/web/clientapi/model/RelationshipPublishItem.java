package io.lumify.web.clientapi.model;

import io.lumify.web.clientapi.model.util.ClientApiConverter;

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
}
