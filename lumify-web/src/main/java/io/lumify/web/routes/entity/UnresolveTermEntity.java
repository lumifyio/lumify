package io.lumify.web.routes.entity;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.termMention.TermMentionModel;
import io.lumify.core.model.termMention.TermMentionRepository;
import io.lumify.core.model.termMention.TermMentionRowKey;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.model.workspace.diff.SandboxStatus;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.core.security.LumifyVisibilityProperties;
import io.lumify.core.security.VisibilityTranslator;
import io.lumify.core.user.User;
import io.lumify.core.util.GraphUtil;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.web.BaseRequestHandler;
import io.lumify.web.routes.workspace.WorkspaceHelper;
import com.altamiracorp.miniweb.HandlerChain;
import org.securegraph.Authorizations;
import org.securegraph.Edge;
import org.securegraph.Graph;
import org.securegraph.Vertex;
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
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration,
            final WorkspaceHelper workspaceHelper) {
        super(userRepository, workspaceRepository, configuration);
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
        final String edgeId = getRequiredParameter(request, "edgeId");
        final String rowKey = getOptionalParameter(request, "rowKey");

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
            visibilityJson = LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.getPropertyValue(edge);
            visibilityJson = GraphUtil.updateVisibilityJsonRemoveFromWorkspace(visibilityJson, workspaceId);
        } else {
            visibilityJson = LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.getPropertyValue(resolvedVertex);
            visibilityJson = GraphUtil.updateVisibilityJsonRemoveFromWorkspace(visibilityJson, workspaceId);
        }
        LumifyVisibility lumifyVisibility = visibilityTranslator.toVisibility(visibilityJson);

        String propertyKey = "";
        if (rowKey != null) {
            TermMentionRowKey analyzedTermMentionRowKey = new TermMentionRowKey(rowKey);
            propertyKey = analyzedTermMentionRowKey.getPropertyKey();
        }

        TermMentionRowKey termMentionRowKey = new TermMentionRowKey(artifactId, propertyKey, mentionStart, mentionEnd, edgeId);
        TermMentionModel termMention = termMentionRepository.findByRowKey(termMentionRowKey.toString(), modelUserContext);
        TermMentionModel analyzedTermMention = termMentionRepository.findByRowKey(new TermMentionRowKey(artifactId, propertyKey, mentionStart, mentionEnd).toString(), modelUserContext);
        JSONObject result = workspaceHelper.unresolveTerm(resolvedVertex, edgeId, termMention, analyzedTermMention, lumifyVisibility, modelUserContext, user, authorizations, workspaceId);

        respondWithJson(response, result);
    }
}
