package com.altamiracorp.lumify.web.routes.vertex;

import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.model.graph.GraphRepository;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class VertexRelationshipRemoval extends BaseRequestHandler {
    private final GraphRepository graphRepository;
    private final AuditRepository auditRepository;
    private final OntologyRepository ontologyRepository;

    @Inject
    public VertexRelationshipRemoval(final GraphRepository graphRepo, final AuditRepository auditRepo, final OntologyRepository ontologyRepo) {
        graphRepository = graphRepo;
        auditRepository = auditRepo;
        ontologyRepository = ontologyRepo;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String source = getRequiredParameter(request, "sourceId");
        final String target = getRequiredParameter(request, "targetId");
        final String label = getRequiredParameter(request, "label");

        User user = getUser(request);
        graphRepository.removeRelationship(source, target, label, user);

        GraphVertex sourceVertex = graphRepository.findVertex(source, user);
        GraphVertex destVertex = graphRepository.findVertex(target, user);
        String displayName = ontologyRepository.getDisplayNameForLabel(label, user);
        // TODO: replace "" when we implement commenting on ui
        auditRepository.auditRelationships(AuditAction.DELETE.toString(), sourceVertex, destVertex, displayName, "", "", user);

        JSONObject resultJson = new JSONObject();
        resultJson.put("success", true);

        respondWithJson(response, resultJson);
    }
}
