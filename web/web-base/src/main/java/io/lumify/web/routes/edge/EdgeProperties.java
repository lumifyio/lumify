package io.lumify.web.routes.edge;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.ClientApiConverter;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import io.lumify.web.clientapi.model.ClientApiEdge;
import org.securegraph.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class EdgeProperties extends BaseRequestHandler {
    private final Graph graph;
    private final OntologyRepository ontologyRepository;

    @Inject
    public EdgeProperties(
            final OntologyRepository ontologyRepository,
            final Graph graph,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
        this.ontologyRepository = ontologyRepository;
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String graphEdgeId = getAttributeString(request, "graphEdgeId");

        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getActiveWorkspaceId(request);

        Edge edge = graph.getEdge(graphEdgeId, authorizations);
        if (edge == null) {
            super.respondWithNotFound(response, "Could not find edge: " + graphEdgeId);
            return;
        }
        Vertex sourceVertex = edge.getVertex(Direction.OUT, authorizations);
        Vertex targetVertex = edge.getVertex(Direction.IN, authorizations);

        ClientApiEdge results = ClientApiConverter.toClientApiEdgeWithVertexData(edge, sourceVertex, targetVertex, workspaceId, authorizations);

        respondWithClientApiObject(response, results);
    }
}
