package io.lumify.web.routes.vertex;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.ClientApiConverter;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import io.lumify.web.clientapi.model.ClientApiVertexEdges;
import org.securegraph.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class VertexEdges extends BaseRequestHandler {
    private final Graph graph;

    @Inject
    public VertexEdges(
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

        String graphVertexId = getAttributeString(request, "graphVertexId");
        long offset = getOptionalParameterLong(request, "offset", 0);
        long size = getOptionalParameterLong(request, "size", 25);
        String edgeLabel = getOptionalParameter(request, "edgeLabel");

        Vertex vertex = graph.getVertex(graphVertexId, authorizations);
        if (vertex == null) {
            respondWithNotFound(response);
            return;
        }

        Iterable<Edge> edges;
        if (edgeLabel == null) {
            edges = vertex.getEdges(Direction.BOTH, authorizations);
        } else {
            edges = vertex.getEdges(Direction.BOTH, edgeLabel, authorizations);
        }

        ClientApiVertexEdges result = new ClientApiVertexEdges();
        long referencesAdded = 0, skipped = 0, totalReferences = 0;
        for (Edge edge : edges) {
            totalReferences++;
            if (referencesAdded >= size) {
                continue;
            }

            if (skipped < offset) {
                skipped++;
                continue;
            }

            Vertex otherVertex = edge.getOtherVertex(vertex.getId(), authorizations);
            if (otherVertex == null) { // user doesn't have access to other side of edge
                continue;
            }

            ClientApiVertexEdges.Edge clientApiEdge = new ClientApiVertexEdges.Edge();
            clientApiEdge.setRelationship(ClientApiConverter.toClientApiEdge(edge, workspaceId, authorizations));
            clientApiEdge.setVertex(ClientApiConverter.toClientApiVertex(otherVertex, workspaceId, authorizations));
            result.getRelationships().add(clientApiEdge);
            referencesAdded++;
        }
        result.setTotalReferences(totalReferences);

        respondWithClientApiObject(response, result);
    }
}
