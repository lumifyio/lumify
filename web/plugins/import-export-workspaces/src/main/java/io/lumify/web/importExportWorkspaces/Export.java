package io.lumify.web.importExportWorkspaces;

import io.lumify.miniweb.HandlerChain;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.Workspace;
import io.lumify.core.model.workspace.WorkspaceEntity;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.securegraph.model.workspace.SecureGraphWorkspaceRepository;
import io.lumify.web.BaseRequestHandler;
import org.securegraph.Authorizations;
import org.securegraph.Edge;
import org.securegraph.Graph;
import org.securegraph.Vertex;
import org.securegraph.tools.GraphBackup;
import org.securegraph.util.ConvertingIterable;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.securegraph.util.IterableUtils.toList;

public class Export extends BaseRequestHandler {
    private final SecureGraphWorkspaceRepository workspaceRepository;
    private final Graph graph;

    @Inject
    public Export(
            UserRepository userRepository,
            Configuration configuration,
            WorkspaceRepository workspaceRepository,
            Graph graph) {
        super(userRepository, workspaceRepository, configuration);
        this.workspaceRepository = (SecureGraphWorkspaceRepository) workspaceRepository;
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String workspaceId = getRequiredParameter(request, "workspaceId");

        User user = getUser(request);
        Workspace workspace = this.workspaceRepository.findById(workspaceId, user);
        if (workspace == null) {
            respondWithNotFound(response);
            return;
        }

        Authorizations authorizations = getUserRepository().getAuthorizations(user, UserRepository.VISIBILITY_STRING, WorkspaceRepository.VISIBILITY_STRING, workspace.getWorkspaceId());

        List<String> workspaceEntityIds = toList(getWorkspaceEntityIds(user, workspace));

        // create this array so that we get the relationships from workspace to entities
        ArrayList<String> workspaceEntityIdsAndWorkspaceId = new ArrayList<String>(workspaceEntityIds);
        workspaceEntityIdsAndWorkspaceId.add(workspace.getWorkspaceId());

        Vertex workspaceVertex = this.workspaceRepository.getVertex(workspace.getWorkspaceId(), user);
        Iterable<Vertex> vertices = graph.getVertices(workspaceEntityIds, authorizations);
        Iterable<Edge> edges = graph.getEdges(graph.findRelatedEdges(workspaceEntityIdsAndWorkspaceId, authorizations), authorizations);

        response.addHeader("Content-Disposition", "attachment; filename=" + workspace.getDisplayTitle() + ".lumifyWorkspace");

        OutputStream out = response.getOutputStream();
        try {
            GraphBackup graphBackup = new GraphBackup();
            graphBackup.saveVertex(workspaceVertex, out);
            graphBackup.save(vertices, edges, out);
        } finally {
            out.close();
        }
    }

    private Iterable<String> getWorkspaceEntityIds(final User user, final Workspace workspace) {
        return new ConvertingIterable<WorkspaceEntity, String>(this.workspaceRepository.findEntities(workspace, user)) {
            @Override
            protected String convert(WorkspaceEntity o) {
                return o.getEntityVertexId();
            }
        };
    }
}
