package com.altamiracorp.lumify.sql.model.workspace;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "workspace_vertex")
@AssociationOverrides({@AssociationOverride(name = "sqlWorkspaceVertexId.vertex", joinColumns = @JoinColumn(name = "vertex_id")),
        @AssociationOverride(name = "sqlWorkspaceVertexId.workspace", joinColumns = @JoinColumn(name = "workspace_id"))})
public class SqlWorkspaceVertex implements Serializable {
    public static final long serialVersionUID = 1L;
    @EmbeddedId
    private SqlWorkspaceVertexId sqlWorkspaceVertexId = new SqlWorkspaceVertexId();
    @Column(name = "graphPositionX")
    private int graphPositionX;
    @Column(name = "graphPositionY")
    private int graphPositionY;
    @Column(name = "visible", columnDefinition = "TINYINT(1)")
    private boolean visible;

    public SqlWorkspaceVertexId getSqlWorkspaceVertexId() {
        return sqlWorkspaceVertexId;
    }

    public void setSqlWorkspaceVertexId(SqlWorkspaceVertexId sqlWorkspaceVertexId) {
        this.sqlWorkspaceVertexId = sqlWorkspaceVertexId;
    }

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

    @Transient
    public SqlVertex getSqlVertex() {
        return getSqlWorkspaceVertexId().getVertex();
    }

    public void setSqlVertex(SqlVertex sqlVertex) {
        getSqlWorkspaceVertexId().setVertex(sqlVertex);
    }

    @Transient
    public SqlWorkspace getSqlWorkspace() {
        return getSqlWorkspaceVertexId().getWorkspace();
    }

    public void setSqlWorkspace(SqlWorkspace sqlWorkspace) {
        getSqlWorkspaceVertexId().setWorkspace(sqlWorkspace);
    }
}
