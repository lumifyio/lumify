package io.lumify.core.model.workspace;

import org.securegraph.Authorizations;
import org.securegraph.Graph;
import org.securegraph.Vertex;
import org.securegraph.util.ConvertingIterable;

public class WorkspaceEntity {
    private final Object entityVertexId;
    private final boolean visible;
    private final Integer graphPositionX;
    private final Integer graphPositionY;

    public WorkspaceEntity(Object entityVertexId, boolean visible, Integer graphPositionX, Integer graphPositionY) {
        this.entityVertexId = entityVertexId;
        this.visible = visible;
        this.graphPositionX = graphPositionX;
        this.graphPositionY = graphPositionY;
    }

    public Object getEntityVertexId() {
        return entityVertexId;
    }

    public Integer getGraphPositionX() {
        return graphPositionX;
    }

    public Integer getGraphPositionY() {
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
