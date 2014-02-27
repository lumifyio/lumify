package com.altamiracorp.lumify.web.routes.relationship;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.config.SandboxLevel;
import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.security.VisibilityTranslator;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.GraphUtil;
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
    private final Configuration configuration;
    private final VisibilityTranslator visibilityTranslator;

    @Inject
    public RelationshipCreate(
            final Graph graph,
            final AuditRepository auditRepository,
            final OntologyRepository ontologyRepository,
            final UserRepository userRepository,
            final VisibilityTranslator visibilityTranslator,
            final Configuration configuration) {
        this.graph = graph;
        this.auditRepository = auditRepository;
        this.ontologyRepository = ontologyRepository;
        this.userRepository = userRepository;
        this.visibilityTranslator = visibilityTranslator;
        this.configuration = configuration;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String sourceGraphVertexId = getRequiredParameter(request, "sourceGraphVertexId");
        final String destGraphVertexId = getRequiredParameter(request, "destGraphVertexId");
        final String predicateLabel = getRequiredParameter(request, "predicateLabel");
        final String visibilitySource = getOptionalParameter(request, "visibilitySource"); // TODO change this to required when the front end is complete

        String workspaceId;
        if (this.configuration.getSandboxLevel() == SandboxLevel.WORKSPACE) {
            workspaceId = getWorkspaceId(request);
        } else {
            workspaceId = null;
        }

        User user = getUser(request);
        Authorizations authorizations = userRepository.getAuthorizations(user);
        String relationshipDisplayName = ontologyRepository.getDisplayNameForLabel(predicateLabel);
        Vertex destVertex = graph.getVertex(destGraphVertexId, authorizations);
        Vertex sourceVertex = graph.getVertex(sourceGraphVertexId, authorizations);

        Edge edge = GraphUtil.addEdge(graph, sourceVertex, destVertex, predicateLabel, visibilitySource, workspaceId, visibilityTranslator, authorizations);

        // TODO: replace second "" when we implement commenting on ui
        auditRepository.auditRelationship(AuditAction.CREATE, sourceVertex, destVertex, relationshipDisplayName, "", "", user, new Visibility(""));

        graph.flush();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Statement created:\n" + toJson(edge).toString(2));
        }

        respondWithJson(response, toJson(edge));
    }
}
