package com.altamiracorp.lumify.sql.model.workspace;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "workspace_vertex")
public class SqlWorkspaceVertex implements Serializable {
    public static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "workspace_vertex_id", unique = true)
    private int workspaceVertexId;
    @Column(name = "graph_position_x")
    private int graphPositionX;
    @Column(name = "graph_position_y")
    private int graphPositionY;
    @Column(name = "visible", columnDefinition = "TINYINT(1)")
    private boolean visible;
    @Column(name = "vertex_id")
    private String vertexId;
    @ManyToOne
    @PrimaryKeyJoinColumn (name="workspace_id")
    private SqlWorkspace workspace;

    public int getGraphPositionY() {
        return graphPositionY;
    }

    public void setGraphPositionY(int graphPositionY) {
        this.graphPositionY = graphPositionY;
    }

    public int getGraphPositionX() {
        return graphPositionX;
    }

    public void setGraphPositionX(int graphPositionX) {
        this.graphPositionX = graphPositionX;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public SqlWorkspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(SqlWorkspace workspace) {
        this.workspace = workspace;
    }

    public String getVertexId() {
        return vertexId;
    }

    public void setVertexId(String vertexId) {
        this.vertexId = vertexId;
    }

    public int getWorkspaceVertexId() {
        return workspaceVertexId;
    }

    public void setWorkspaceVertexId(int workspaceVertexId) {
        this.workspaceVertexId = workspaceVertexId;
    }
}
