package io.lumify.sql.model.workspace;

import javax.persistence.*;

@Entity
@Table(name = "workspace_vertex")
public class SqlWorkspaceVertex {
    private int workspaceVertexId;
    private Integer graphPositionX;
    private Integer graphPositionY;
    private boolean visible;
    private String vertexId;
    private SqlWorkspace workspace;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "workspace_vertex_id", unique = true)
    public int getWorkspaceVertexId() {
        return workspaceVertexId;
    }

    public void setWorkspaceVertexId(int workspaceVertexId) {
        this.workspaceVertexId = workspaceVertexId;
    }

    @Column (name = "graph_position_x")
    public Integer getGraphPositionX() {
        return graphPositionX;
    }

    public void setGraphPositionX(Integer graphPositionX) {
        this.graphPositionX = graphPositionX;
    }

    @Column (name = "graph_position_y")
    public Integer getGraphPositionY() {
        return graphPositionY;
    }

    public void setGraphPositionY(Integer graphPositionY) {
        this.graphPositionY = graphPositionY;
    }

    @Column (name = "is_visible")
    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Column (name = "vertex_id")
    public String getVertexId() {
        return vertexId;
    }

    public void setVertexId(String vertexId) {
        this.vertexId = vertexId;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspaceId",  referencedColumnName = "workspace_id", nullable = false)
    public SqlWorkspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(SqlWorkspace workspace) {
        this.workspace = workspace;
    }
}
