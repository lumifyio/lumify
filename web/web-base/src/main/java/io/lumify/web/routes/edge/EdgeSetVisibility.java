package io.lumify.web.routes.edge;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.audit.AuditAction;
import io.lumify.core.model.audit.AuditRepository;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.security.VisibilityTranslator;
import io.lumify.core.user.User;
import io.lumify.core.util.ClientApiConverter;
import io.lumify.core.util.GraphUtil;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import org.securegraph.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class EdgeSetVisibility extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(EdgeSetVisibility.class);
    private final Graph graph;
    private final VisibilityTranslator visibilityTranslator;
    private final WorkQueueRepository workQueueRepository;
    private final AuditRepository auditRepository;

    @Inject
    public EdgeSetVisibility(
            final Graph graph,
            final UserRepository userRepository,
            final Configuration configuration,
            final VisibilityTranslator visibilityTranslator,
            final WorkspaceRepository workspaceRepository,
            final WorkQueueRepository workQueueRepository,
            final AuditRepository auditRepository) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
        this.visibilityTranslator = visibilityTranslator;
        this.workQueueRepository = workQueueRepository;
        this.auditRepository = auditRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String graphEdgeId = getAttributeString(request, "graphEdgeId");
        final String visibilitySource = getRequiredParameter(request, "visibilitySource");

        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getActiveWorkspaceId(request);

        Edge graphEdge = graph.getEdge(graphEdgeId, authorizations);
        if (graphEdge == null) {
            respondWithNotFound(response);
            return;
        }

        if (!graph.isVisibilityValid(new Visibility(visibilitySource), authorizations)) {
            LOGGER.warn("%s is not a valid visibility for %s user", visibilitySource, user.getDisplayName());
            respondWithBadRequest(response, "visibilitySource", getString(request, "visibility.invalid"));
            chain.next(request, response);
            return;
        }

        LOGGER.info("changing edge (%s) visibility source to %s", graphEdge.getId(), visibilitySource);

        GraphUtil.VisibilityAndElementMutation<Edge> setPropertyResult = GraphUtil.updateElementVisibilitySource(visibilityTranslator, graphEdge, GraphUtil.getSandboxStatus(graphEdge, workspaceId), visibilitySource, workspaceId, authorizations);
        auditRepository.auditEdgeElementMutation(AuditAction.UPDATE, setPropertyResult.elementMutation, graphEdge,
                graphEdge.getVertex(Direction.OUT, authorizations), graphEdge.getVertex(Direction.IN, authorizations), "", user, setPropertyResult.visibility.getVisibility());

        this.graph.flush();

        this.workQueueRepository.pushGraphPropertyQueue(graphEdge, null,
                LumifyProperties.VISIBILITY_JSON.getPropertyName(), workspaceId, visibilitySource);

        respondWithClientApiObject(response, ClientApiConverter.toClientApi(graphEdge, workspaceId, authorizations));
    }
}
