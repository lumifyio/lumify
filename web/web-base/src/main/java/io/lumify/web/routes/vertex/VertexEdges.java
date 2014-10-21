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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        Map<String, Vertex> accessibleVertexIds = getAccessibleVertices(vertex, edges, authorizations);

        for (Edge edge : edges) {
            String otherVertexId = edge.getOtherVertexId(vertex.getId());
            Vertex otherVertex = accessibleVertexIds.get(otherVertexId);
            if (otherVertex == null) {
                continue;
            }

            totalReferences++;
            if (referencesAdded >= size) {
                continue;
            }

            if (skipped < offset) {
                skipped++;
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

    // a user may have access to an edge but not the vertex on the other end.
    // so we need to get all the vertices to see if we have access.
    public Map<String, Vertex> getAccessibleVertices(Vertex vertex, Iterable<Edge> edges, Authorizations authorizations) {
        List<String> vertexIds = new ArrayList<String>();
        for (Edge edge : edges) {
            vertexIds.add(edge.getOtherVertexId(vertex.getId()));
        }
        Iterable<Vertex> accessibleVertices = graph.getVertices(vertexIds, authorizations);
        Map<String, Vertex> accessibleVertexIds = new HashMap<String, Vertex>();
        for (Vertex v : accessibleVertices) {
            accessibleVertexIds.put(v.getId(), v);
        }
        return accessibleVertexIds;
    }
}
