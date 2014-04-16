package com.altamiracorp.lumify.web.routes.relationship;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.security.VisibilityTranslator;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.GraphUtil;
import com.altamiracorp.lumify.core.util.JsonSerializer;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.*;
import com.google.inject.Inject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RelationshipCreate extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(RelationshipCreate.class);

    private final Graph graph;
    private final AuditRepository auditRepository;
    private final VisibilityTranslator visibilityTranslator;

    @Inject
    public RelationshipCreate(
            final Graph graph,
            final AuditRepository auditRepository,
            final OntologyRepository ontologyRepository,
            final VisibilityTranslator visibilityTranslator,
            final UserRepository userRepository,
            final Configuration configuration) {
        super(userRepository, configuration);
        this.graph = graph;
        this.auditRepository = auditRepository;
        this.visibilityTranslator = visibilityTranslator;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String sourceGraphVertexId = getRequiredParameter(request, "sourceGraphVertexId");
        final String destGraphVertexId = getRequiredParameter(request, "destGraphVertexId");
        final String predicateLabel = getRequiredParameter(request, "predicateLabel");
        final String visibilitySource = getOptionalParameter(request, "visibilitySource");
        final String justificationText = getOptionalParameter(request, "justificationText");
        final String sourceInfo = getOptionalParameter(request, "sourceInfo");

        String workspaceId = getActiveWorkspaceId(request);

        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        Vertex destVertex = graph.getVertex(destGraphVertexId, authorizations);
        Vertex sourceVertex = graph.getVertex(sourceGraphVertexId, authorizations);

        if (!graph.isVisibilityValid(new Visibility(visibilitySource), authorizations)) {
            LOGGER.warn("%s is not a valid visibility for %s user", visibilitySource, user.getDisplayName());
            respondWithBadRequest(response, "visibilitySource", STRINGS.getString("visibility.invalid"));
            chain.next(request, response);
            return;
        }

        Edge edge = GraphUtil.addEdge(graph, sourceVertex, destVertex, predicateLabel, justificationText, sourceInfo, visibilitySource, workspaceId, visibilityTranslator, authorizations);

        // TODO: replace second "" when we implement commenting on ui
        auditRepository.auditRelationship(AuditAction.CREATE, sourceVertex, destVertex, edge, "", "",
                user, edge.getVisibility());

        graph.flush();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Statement created:\n" + JsonSerializer.toJson(edge, workspaceId).toString(2));
        }

        respondWithJson(response, JsonSerializer.toJson(edge, workspaceId));
    }
}
