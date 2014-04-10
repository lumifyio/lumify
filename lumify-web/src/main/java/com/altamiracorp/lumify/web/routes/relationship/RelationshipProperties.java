package com.altamiracorp.lumify.web.routes.relationship;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.JsonSerializer;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.*;
import com.google.inject.Inject;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RelationshipProperties extends BaseRequestHandler {
    private final Graph graph;
    private final OntologyRepository ontologyRepository;

    @Inject
    public RelationshipProperties(
            final OntologyRepository ontologyRepository,
            final Graph graph,
            final UserRepository userRepository,
            final Configuration configuration) {
        super(userRepository, configuration);
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
        Vertex sourceVertex = edge.getVertex(Direction.OUT, authorizations);
        Vertex targetVertex = edge.getVertex(Direction.IN, authorizations);

        JSONObject results = JsonSerializer.toJson(edge, workspaceId);
        results.put("source", JsonSerializer.toJson(sourceVertex, workspaceId));
        results.put("target", JsonSerializer.toJson(targetVertex, workspaceId));

        respondWithJson(response, results);
    }
}
