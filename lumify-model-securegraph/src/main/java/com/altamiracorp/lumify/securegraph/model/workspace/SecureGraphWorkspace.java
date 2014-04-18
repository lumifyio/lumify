package com.altamiracorp.lumify.securegraph.model.workspace;

import com.altamiracorp.lumify.core.model.workspace.Workspace;
import com.altamiracorp.lumify.core.model.workspace.WorkspaceLumifyProperties;
import com.altamiracorp.securegraph.Authorizations;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;

public class SecureGraphWorkspace implements Workspace {
    private static final long serialVersionUID = -1692706831716776578L;
    private String displayTitle;
    private String workspaceId;
    private transient Vertex workspaceVertex;

    public SecureGraphWorkspace(Vertex workspaceVertex) {
        this.displayTitle = WorkspaceLumifyProperties.TITLE.getPropertyValue(workspaceVertex);
        this.workspaceId = workspaceVertex.getId().toString();
        this.workspaceVertex = workspaceVertex;
    }

    @Override
    public String getId() {
        return workspaceId;
    }

    @Override
    public String getDisplayTitle() {
        return displayTitle;
    }

    public Vertex getVertex(Graph graph, Authorizations authorizations) {
        if (this.workspaceVertex == null) {
            this.workspaceVertex = graph.getVertex(getId(), authorizations);
        }
        return this.workspaceVertex;
    }
}