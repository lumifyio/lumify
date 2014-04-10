package com.altamiracorp.lumify.sql.model.workspace;


import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "vertex")
public class SqlVertex {
    public static final long serialVersionUID = 1L;
    @Id
    @Column(name = "vertex_id", unique = true)
    private String vertexId;
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "sqlWorkspaceVertexId.vertex")
    private Set<SqlWorkspaceVertex> sqlWorkspaceVertices = new HashSet<SqlWorkspaceVertex>(0);

    public String getVertexId() {
        return vertexId;
    }

    public void setVertexId(String vertexId) {
        this.vertexId = vertexId;
    }

    public Set<SqlWorkspaceVertex> getSqlWorkspaceVertices() {
        return sqlWorkspaceVertices;
    }

    public void setSqlWorkspaceVertices(Set<SqlWorkspaceVertex> sqlWorkspaceVertices) {
        this.sqlWorkspaceVertices = sqlWorkspaceVertices;
    }
}
