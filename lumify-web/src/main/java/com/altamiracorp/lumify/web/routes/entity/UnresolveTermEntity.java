package com.altamiracorp.lumify.web.routes.entity;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.termMention.TermMentionModel;
import com.altamiracorp.lumify.core.model.termMention.TermMentionRepository;
import com.altamiracorp.lumify.core.model.termMention.TermMentionRowKey;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.model.workspace.diff.SandboxStatus;
import com.altamiracorp.lumify.core.security.LumifyVisibility;
import com.altamiracorp.lumify.core.security.LumifyVisibilityProperties;
import com.altamiracorp.lumify.core.security.VisibilityTranslator;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.GraphUtil;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.lumify.web.routes.workspace.WorkspaceHelper;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Authorizations;
import com.altamiracorp.securegraph.Edge;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.google.inject.Inject;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UnresolveTermEntity extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(UnresolveTermEntity.class);
    private final TermMentionRepository termMentionRepository;
    private final Graph graph;
    private final VisibilityTranslator visibilityTranslator;
    private final UserRepository userRepository;
    private final WorkspaceHelper workspaceHelper;

    @Inject
    public UnresolveTermEntity(
            final TermMentionRepository termMentionRepository,
            final Graph graph,
            final UserRepository userRepository,
            final VisibilityTranslator visibilityTranslator,
            final Configuration configuration,
            final WorkspaceHelper workspaceHelper) {
        super(userRepository, configuration);
        this.termMentionRepository = termMentionRepository;
        this.graph = graph;
        this.visibilityTranslator = visibilityTranslator;
        this.userRepository = userRepository;
        this.workspaceHelper = workspaceHelper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        // required parameters
        final String artifactId = getRequiredParameter(request, "artifactId");
        final long mentionStart = getRequiredParameterAsLong(request, "mentionStart");
        final long mentionEnd = getRequiredParameterAsLong(request, "mentionEnd");
        final String sign = getRequiredParameter(request, "sign");
        final String conceptId = getRequiredParameter(request, "conceptId");
        final String graphVertexId = getRequiredParameter(request, "graphVertexId");
        final String visibilitySource = getRequiredParameter(request, "visibilitySource");
        final String edgeId = getRequiredParameter(request, "edgeId");

        LOGGER.debug(
                "UnresolveTermEntity (artifactId: %s, mentionStart: %d, mentionEnd: %d, sign: %s, conceptId: %s, graphVertexId: %s)",
                artifactId,
                mentionStart,
                mentionEnd,
                sign,
                conceptId,
                graphVertexId);

        String workspaceId = getActiveWorkspaceId(request);
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        ModelUserContext modelUserContext = userRepository.getModelUserContext(authorizations, workspaceId);

        Vertex resolvedVertex = graph.getVertex(graphVertexId, authorizations);
        Edge edge = graph.getEdge(edgeId, authorizations);

        SandboxStatus vertexSandboxStatus = GraphUtil.getSandboxStatus(resolvedVertex, workspaceId);
        SandboxStatus edgeSandboxStatus = GraphUtil.getSandboxStatus(edge, workspaceId);
        if (vertexSandboxStatus == SandboxStatus.PUBLIC && edgeSandboxStatus == SandboxStatus.PUBLIC) {
            LOGGER.warn("Can not unresolve a public entity");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            chain.next(request, response);
            return;
        }

        JSONObject visibilityJson;
        if (vertexSandboxStatus == SandboxStatus.PUBLIC) {
            visibilityJson = GraphUtil.updateVisibilityJsonRemoveFromWorkspace(edge.getPropertyValue(LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.toString()).toString(), workspaceId);
        } else {
            visibilityJson = GraphUtil.updateVisibilityJsonRemoveFromWorkspace(resolvedVertex.getPropertyValue(LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.toString()).toString(), workspaceId);
        }
        LumifyVisibility lumifyVisibility = visibilityTranslator.toVisibility(visibilityJson);

        TermMentionRowKey termMentionRowKey = new TermMentionRowKey(artifactId, mentionStart, mentionEnd, edgeId);
        TermMentionModel termMention = termMentionRepository.findByRowKey(termMentionRowKey.toString(), modelUserContext);
        TermMentionModel analyzedTermMention = termMentionRepository.findByRowKey(new TermMentionRowKey(artifactId, mentionStart, mentionEnd).toString(), modelUserContext);
        JSONObject result = workspaceHelper.unresolveTerm(resolvedVertex, edgeId, termMention, analyzedTermMention, lumifyVisibility, modelUserContext, user, authorizations);

        respondWithJson(response, result);
    }
}
