package io.lumify.core.model.workspace;

import org.securegraph.Authorizations;
import org.securegraph.Graph;
import org.securegraph.Vertex;
import org.securegraph.util.ConvertingIterable;

public class WorkspaceEntity {
    private final String entityVertexId;
    private final boolean visible;
    private final Integer graphPositionX;
    private final Integer graphPositionY;
    private final String graphLayoutJson;

    public WorkspaceEntity(String entityVertexId, boolean visible, Integer graphPositionX, Integer graphPositionY, String graphLayoutJson) {
        this.entityVertexId = entityVertexId;
        this.visible = visible;
        this.graphPositionX = graphPositionX;
        this.graphPositionY = graphPositionY;
        this.graphLayoutJson = graphLayoutJson;
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

    public String getGraphLayoutJson() {
        return graphLayoutJson;
    }

    public boolean isVisible() {
        return visible;
    }

    public static Iterable<Vertex> toVertices(Graph graph, Iterable<WorkspaceEntity> workspaceEntities, Authorizations authorizations) {
        Iterable<String> vertexIds = toVertexIds(workspaceEntities);
        return graph.getVertices(vertexIds, authorizations);
    }

    public static Iterable<String> toVertexIds(Iterable<WorkspaceEntity> workspaceEntities) {
        return new ConvertingIterable<WorkspaceEntity, String>(workspaceEntities) {
            @Override
            protected String convert(WorkspaceEntity o) {
                return o.getEntityVertexId();
            }
        };
    }
}
