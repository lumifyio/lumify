package io.lumify.web.routes.vertex;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.ClientApiConverter;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import io.lumify.web.clientapi.model.ClientApiElement;
import org.securegraph.Authorizations;
import org.securegraph.Graph;
import org.securegraph.Vertex;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class VertexProperties extends BaseRequestHandler {
    private final Graph graph;

    @Inject
    public VertexProperties(
            final Graph graph,
            final UserRepository userRepository,
            final Configuration configuration,
            final WorkspaceRepository workspaceRepository) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String graphVertexId = getAttributeString(request, "graphVertexId");
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getActiveWorkspaceId(request);

        ClientApiElement element = handle(graphVertexId, workspaceId, authorizations);
        respondWithClientApiObject(response, element);
    }

    private ClientApiElement handle(String graphVertexId, String workspaceId, Authorizations authorizations) {
        Vertex vertex = graph.getVertex(graphVertexId, authorizations);
        if (vertex == null) {
            return null;
        }
        return ClientApiConverter.toClientApi(vertex, workspaceId, authorizations);
    }
}
