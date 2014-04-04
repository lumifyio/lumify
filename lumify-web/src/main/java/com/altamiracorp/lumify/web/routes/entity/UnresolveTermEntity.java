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
import com.altamiracorp.lumify.core.user.UserProvider;
import com.altamiracorp.lumify.core.util.GraphUtil;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.lumify.web.routes.workspace.WorkspaceHelper;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Authorizations;
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
    private final UserProvider userProvider;
    private final WorkspaceHelper workspaceHelper;

    @Inject
    public UnresolveTermEntity(
            final TermMentionRepository termMentionRepository,
            final Graph graph,
            final UserRepository userRepository,
            final VisibilityTranslator visibilityTranslator,
            final Configuration configuration,
            final UserProvider userProvider,
            final WorkspaceHelper workspaceHelper) {
        super(userRepository, configuration);
        this.termMentionRepository = termMentionRepository;
        this.graph = graph;
        this.visibilityTranslator = visibilityTranslator;
        this.userProvider = userProvider;
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
        ModelUserContext modelUserContext = userProvider.getModelUserContext(authorizations, getActiveWorkspaceId(request));

        Vertex resolvedVertex = graph.getVertex(graphVertexId, authorizations);

        SandboxStatus sandboxStatus = GraphUtil.getSandboxStatus(resolvedVertex, workspaceId);
        if (sandboxStatus == SandboxStatus.PUBLIC) {
            LOGGER.warn("Can not unresolve a public entity");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            chain.next(request, response);
            return;
        }

        JSONObject visibilityJson = GraphUtil.updateVisibilityJsonRemoveFromWorkspace(resolvedVertex.getPropertyValue(LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.toString()).toString(), workspaceId);
        LumifyVisibility lumifyVisibility = visibilityTranslator.toVisibility(visibilityJson);

        TermMentionRowKey termMentionRowKey = new TermMentionRowKey(artifactId, mentionStart, mentionEnd, graphVertexId);
        TermMentionModel termMention = termMentionRepository.findByRowKey(termMentionRowKey.toString(), modelUserContext);
        TermMentionModel analyzedTermMention = termMentionRepository.findByRowKey(new TermMentionRowKey(artifactId, mentionStart, mentionEnd).toString(), modelUserContext);
        JSONObject result = workspaceHelper.unresolveTerm(resolvedVertex, termMention, analyzedTermMention, lumifyVisibility, modelUserContext, user, authorizations);

        respondWithJson(response, result);
    }
}
