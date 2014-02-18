package com.altamiracorp.lumify.core.model.workspace;

public class WorkspaceEntity {
    private final Object entityVertexId;
    private final int graphPositionX;
    private final int graphPositionY;

    public WorkspaceEntity(Object entityVertexId, int graphPositionX, int graphPositionY) {
        this.entityVertexId = entityVertexId;
        this.graphPositionX = graphPositionX;
        this.graphPositionY = graphPositionY;
    }

    public Object getEntityVertexId() {
        return entityVertexId;
    }

    public int getGraphPositionX() {
        return graphPositionX;
    }

    public int getGraphPositionY() {
        return graphPositionY;
    }
}
