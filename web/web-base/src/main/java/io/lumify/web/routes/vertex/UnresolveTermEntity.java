package io.lumify.web.routes.vertex;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.termMention.TermMentionRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.core.security.VisibilityTranslator;
import io.lumify.core.user.User;
import io.lumify.core.util.GraphUtil;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import io.lumify.web.clientapi.model.SandboxStatus;
import io.lumify.web.clientapi.model.VisibilityJson;
import io.lumify.web.routes.workspace.WorkspaceHelper;
import org.securegraph.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.securegraph.util.IterableUtils.singleOrDefault;

public class UnresolveTermEntity extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(UnresolveTermEntity.class);
    private final TermMentionRepository termMentionRepository;
    private final Graph graph;
    private final VisibilityTranslator visibilityTranslator;
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
        this.workspaceHelper = workspaceHelper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String termMentionId = getRequiredParameter(request, "termMentionId");

        LOGGER.debug("UnresolveTermEntity (termMentionId: %s)", termMentionId);

        String workspaceId = getActiveWorkspaceId(request);
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);

        Vertex termMention = termMentionRepository.findById(termMentionId, authorizations);
        if (termMention == null) {
            respondWithNotFound(response, "Could not find term mention with id: " + termMentionId);
            return;
        }

        Vertex resolvedVertex = singleOrDefault(termMention.getVertices(Direction.OUT, LumifyProperties.TERM_MENTION_LABEL_RESOLVED_TO, authorizations), null);
        if (resolvedVertex == null) {
            respondWithNotFound(response, "Could not find resolved vertex from term mention: " + termMentionId);
            return;
        }

        String edgeId = LumifyProperties.TERM_MENTION_RESOLVED_EDGE_ID.getPropertyValue(termMention);
        Edge edge = graph.getEdge(edgeId, authorizations);
        if (edge == null) {
            respondWithNotFound(response, "Could not find edge " + edgeId + " from term mention: " + termMentionId);
            return;
        }

        SandboxStatus vertexSandboxStatus = GraphUtil.getSandboxStatus(resolvedVertex, workspaceId);
        SandboxStatus edgeSandboxStatus = GraphUtil.getSandboxStatus(edge, workspaceId);
        if (vertexSandboxStatus == SandboxStatus.PUBLIC && edgeSandboxStatus == SandboxStatus.PUBLIC) {
            LOGGER.warn("Can not unresolve a public entity");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            chain.next(request, response);
            return;
        }

        VisibilityJson visibilityJson;
        if (vertexSandboxStatus == SandboxStatus.PUBLIC) {
            visibilityJson = LumifyProperties.VISIBILITY_JSON.getPropertyValue(edge);
            visibilityJson = GraphUtil.updateVisibilityJsonRemoveFromWorkspace(visibilityJson, workspaceId);
        } else {
            visibilityJson = LumifyProperties.VISIBILITY_JSON.getPropertyValue(resolvedVertex);
            visibilityJson = GraphUtil.updateVisibilityJsonRemoveFromWorkspace(visibilityJson, workspaceId);
        }
        LumifyVisibility lumifyVisibility = visibilityTranslator.toVisibility(visibilityJson);

        workspaceHelper.unresolveTerm(resolvedVertex, termMention, lumifyVisibility, user, workspaceId, authorizations);
        respondWithSuccessJson(response);
    }
}
