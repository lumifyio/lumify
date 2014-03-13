package com.altamiracorp.lumify.core.model.workspace;

import com.altamiracorp.securegraph.Authorizations;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.util.ConvertingIterable;

public class WorkspaceEntity {
    private final Object entityVertexId;
    private final boolean visible;
    private final int graphPositionX;
    private final int graphPositionY;

    public WorkspaceEntity(Object entityVertexId, boolean visible, int graphPositionX, int graphPositionY) {
        this.entityVertexId = entityVertexId;
        this.visible = visible;
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

    public boolean isVisible() {
        return visible;
    }

    public static Iterable<Vertex> toVertices(Graph graph, Iterable<WorkspaceEntity> workspaceEntities, Authorizations authorizations) {
        Iterable<Object> vertexIds = toVertexIds(workspaceEntities);
        return graph.getVertices(vertexIds, authorizations);
    }

    public static Iterable<Object> toVertexIds(Iterable<WorkspaceEntity> workspaceEntities) {
        return new ConvertingIterable<WorkspaceEntity, Object>(workspaceEntities) {
            @Override
            protected Object convert(WorkspaceEntity o) {
                return o.getEntityVertexId();
            }
        };
    }
}
