package com.altamiracorp.lumify.web.routes.relationship;

import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.*;
import com.google.inject.Inject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.altamiracorp.lumify.core.util.GraphUtil.toJson;

public class RelationshipCreate extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(RelationshipCreate.class);

    private final Graph graph;
    private final AuditRepository auditRepository;
    private final OntologyRepository ontologyRepository;
    private final UserRepository userRepository;

    @Inject
    public RelationshipCreate(final Graph graph,
                              final AuditRepository auditRepository,
                              final OntologyRepository ontologyRepository,
                              final UserRepository userRepository) {
        this.graph = graph;
        this.auditRepository = auditRepository;
        this.ontologyRepository = ontologyRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        // validate parameters
        final String sourceGraphVertexId = getRequiredParameter(request, "sourceGraphVertexId");
        final String destGraphVertexId = getRequiredParameter(request, "destGraphVertexId");
        final String predicateLabel = getRequiredParameter(request, "predicateLabel");

        User user = getUser(request);
        Authorizations authorizations = userRepository.getAuthorizations(user);
        String relationshipDisplayName = ontologyRepository.getDisplayNameForLabel(predicateLabel);
        Vertex destVertex = graph.getVertex(destGraphVertexId, authorizations);
        Vertex sourceVertex = graph.getVertex(sourceGraphVertexId, authorizations);

        Edge edge = graph.addEdge(sourceVertex, destVertex, predicateLabel, new Visibility(""), authorizations);

        // TODO: replace second "" when we implement commenting on ui
        auditRepository.auditRelationship(AuditAction.CREATE, sourceVertex, destVertex, relationshipDisplayName, "", "", user, new Visibility(""));

        graph.flush();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Statement created:\n" + toJson(edge).toString(2));
        }

        respondWithJson(response, toJson(edge));
    }
}
