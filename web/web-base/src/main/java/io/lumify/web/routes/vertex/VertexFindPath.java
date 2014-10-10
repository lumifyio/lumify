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
import io.lumify.web.clientapi.model.ClientApiVertex;
import io.lumify.web.clientapi.model.ClientApiVertexFindPathResponse;
import org.securegraph.Authorizations;
import org.securegraph.Graph;
import org.securegraph.Path;
import org.securegraph.Vertex;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

public class VertexFindPath extends BaseRequestHandler {
    private final Graph graph;

    @Inject
    public VertexFindPath(
            final Graph graph,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getActiveWorkspaceId(request);

        final String sourceGraphVertexId = getRequiredParameter(request, "sourceGraphVertexId");
        final String destGraphVertexId = getRequiredParameter(request, "destGraphVertexId");
        final int hops = Integer.parseInt(getRequiredParameter(request, "hops"));

        Vertex sourceVertex = graph.getVertex(sourceGraphVertexId, authorizations);
        if (sourceVertex == null) {
            respondWithNotFound(response, "Source vertex not found");
            return;
        }

        Vertex destVertex = graph.getVertex(destGraphVertexId, authorizations);
        if (destVertex == null) {
            respondWithNotFound(response, "Destination vertex not found");
            return;
        }

        ClientApiVertexFindPathResponse results = new ClientApiVertexFindPathResponse();

        Iterable<Path> paths = graph.findPaths(sourceVertex, destVertex, hops, authorizations);
        for (Path path : paths) {
            List<ClientApiElement> clientApiElementPath = ClientApiConverter.toClientApi(graph.getVerticesInOrder(path, authorizations), workspaceId, authorizations);
            List<ClientApiVertex> clientApiVertexPath = new ArrayList<ClientApiVertex>();
            for (ClientApiElement e : clientApiElementPath) {
                clientApiVertexPath.add((ClientApiVertex) e);
            }
            results.getPaths().add(clientApiVertexPath);
        }

        respondWithClientApiObject(response, results);
    }
}

