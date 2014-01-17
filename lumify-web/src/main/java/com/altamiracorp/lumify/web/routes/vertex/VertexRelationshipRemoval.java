package com.altamiracorp.lumify.web.routes.vertex;

import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Direction;
import com.altamiracorp.securegraph.Edge;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.google.inject.Inject;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class VertexRelationshipRemoval extends BaseRequestHandler {
    private final Graph graph;
    private final AuditRepository auditRepository;
    private final OntologyRepository ontologyRepository;

    @Inject
    public VertexRelationshipRemoval(final Graph graph, final AuditRepository auditRepository, final OntologyRepository ontologyRepository) {
        this.graph = graph;
        this.auditRepository = auditRepository;
        this.ontologyRepository = ontologyRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String sourceId = getRequiredParameter(request, "sourceId");
        final String targetId = getRequiredParameter(request, "targetId");
        final String label = getRequiredParameter(request, "label");
        final String edgeId = getRequiredParameter(request, "edgeId");

        User user = getUser(request);
        Vertex sourceVertex = graph.getVertex(sourceId, user.getAuthorizations());
        Vertex destVertex = graph.getVertex(targetId, user.getAuthorizations());

        graph.removeEdge(edgeId, user.getAuthorizations());

        String displayName = ontologyRepository.getDisplayNameForLabel(label);
        // TODO: replace "" when we implement commenting on ui
        auditRepository.auditRelationships(AuditAction.DELETE.toString(), sourceVertex, destVertex, displayName, "", "", user);

        graph.flush();

        JSONObject resultJson = new JSONObject();
        resultJson.put("success", true);

        respondWithJson(response, resultJson);
    }
}
