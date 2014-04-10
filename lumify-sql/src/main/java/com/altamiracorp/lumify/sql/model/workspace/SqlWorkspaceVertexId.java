package com.altamiracorp.lumify.sql.model.workspace;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.ManyToOne;
import java.io.Serializable;

@Embeddable
public class SqlWorkspaceVertexId implements Serializable{
    private static final long serialVersionUID = 1L;
    @ManyToOne(cascade = CascadeType.ALL)
    private SqlWorkspace workspace;
    @ManyToOne (cascade = CascadeType.ALL)
    private SqlVertex vertex;

    public SqlWorkspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(SqlWorkspace workspace) {
        this.workspace = workspace;
    }

    public SqlVertex getVertex() {
        return vertex;
    }

    public void setVertex(SqlVertex vertex) {
        this.vertex = vertex;
    }
}
