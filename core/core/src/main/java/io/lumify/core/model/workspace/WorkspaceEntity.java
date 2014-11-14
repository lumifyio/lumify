package io.lumify.core.model.workspace;

import org.securegraph.Authorizations;
import org.securegraph.FetchHint;
import org.securegraph.Graph;
import org.securegraph.Vertex;
import org.securegraph.util.ConvertingIterable;

public class WorkspaceEntity {
    private final String entityVertexId;
    private final boolean visible;
    private final Integer graphPositionX;
    private final Integer graphPositionY;

    public WorkspaceEntity(String entityVertexId, boolean visible, Integer graphPositionX, Integer graphPositionY) {
        this.entityVertexId = entityVertexId;
        this.visible = visible;
        this.graphPositionX = graphPositionX;
        this.graphPositionY = graphPositionY;
    }

    public String getEntityVertexId() {
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

    public static Iterable<Vertex> toVertices(Graph graph, Iterable<WorkspaceEntity> workspaceEntities, boolean includeHidden, Authorizations authorizations) {
        Iterable<String> vertexIds = toVertexIds(workspaceEntities);
        return graph.getVertices(vertexIds, includeHidden ? FetchHint.ALL_INCLUDING_HIDDEN : FetchHint.ALL, authorizations);
    }

    public static Iterable<String> toVertexIds(Iterable<WorkspaceEntity> workspaceEntities) {
        return new ConvertingIterable<WorkspaceEntity, String>(workspaceEntities) {
            @Override
            protected String convert(WorkspaceEntity o) {
                return o.getEntityVertexId();
            }
        };
    }

    @Override
    public String toString() {
        return "WorkspaceEntity{" +
                "entityVertexId='" + entityVertexId + '\'' +
                '}';
    }
}
