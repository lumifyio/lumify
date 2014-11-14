package io.lumify.securegraph.model.workspace;

import io.lumify.core.model.workspace.Workspace;
import io.lumify.core.model.workspace.WorkspaceLumifyProperties;
import org.securegraph.Authorizations;
import org.securegraph.FetchHint;
import org.securegraph.Graph;
import org.securegraph.Vertex;

public class SecureGraphWorkspace implements Workspace {
    private static final long serialVersionUID = -1692706831716776578L;
    private String displayTitle;
    private String workspaceId;
    private transient Vertex workspaceVertex;

    public SecureGraphWorkspace(Vertex workspaceVertex) {
        this.displayTitle = WorkspaceLumifyProperties.TITLE.getPropertyValue(workspaceVertex);
        this.workspaceId = workspaceVertex.getId();
        this.workspaceVertex = workspaceVertex;
    }

    @Override
    public String getWorkspaceId() {
        return workspaceId;
    }

    @Override
    public String getDisplayTitle() {
        return displayTitle;
    }

    public Vertex getVertex(Graph graph, boolean includeHidden, Authorizations authorizations) {
        if (this.workspaceVertex == null) {
            this.workspaceVertex = graph.getVertex(getWorkspaceId(), includeHidden ? FetchHint.ALL_INCLUDING_HIDDEN : FetchHint.ALL, authorizations);
        }
        return this.workspaceVertex;
    }
}