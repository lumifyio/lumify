package io.lumify.web.routes.vertex;

import io.lumify.core.config.Configuration;
import io.lumify.core.model.audit.AuditAction;
import io.lumify.core.model.audit.AuditRepository;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.security.LumifyVisibilityProperties;
import io.lumify.core.security.VisibilityTranslator;
import io.lumify.core.user.User;
import io.lumify.core.util.GraphUtil;
import io.lumify.core.util.JsonSerializer;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import org.securegraph.*;
import com.google.inject.Inject;
import org.json.JSONObject;
import org.securegraph.mutation.ElementMutation;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

public class VertexSetVisibility extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(VertexSetVisibility.class);
    private final Graph graph;
    private final VisibilityTranslator visibilityTranslator;
    private final WorkQueueRepository workQueueRepository;
    private final AuditRepository auditRepository;

    @Inject
    public VertexSetVisibility(
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
        final String graphVertexId = getAttributeString(request, "graphVertexId");
        final String visibilitySource = getRequiredParameter(request, "visibilitySource");

        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getActiveWorkspaceId(request);

        Vertex graphVertex = graph.getVertex(graphVertexId, authorizations);
        if (graphVertex == null) {
            respondWithNotFound(response);
            return;
        }

        if (!graph.isVisibilityValid(new Visibility(visibilitySource), authorizations)) {
            LOGGER.warn("%s is not a valid visibility for %s user", visibilitySource, user.getDisplayName());
            respondWithBadRequest(response, "visibilitySource", STRINGS.getString("visibility.invalid"));
            chain.next(request, response);
            return;
        }
        LOGGER.info("changing vertex (%s) visibility source to %s", graphVertex.getId().toString(), visibilitySource);

        GraphUtil.VisibilityAndElementMutation<Vertex> setPropertyResult = GraphUtil.updateElementVisibilitySource(visibilityTranslator, graphVertex, GraphUtil.getSandboxStatus(graphVertex, workspaceId), visibilitySource, workspaceId);
        auditRepository.auditVertexElementMutation(AuditAction.UPDATE, setPropertyResult.elementMutation, graphVertex, "", user, setPropertyResult.visibility.getVisibility());

        this.graph.flush();

        this.workQueueRepository.pushGraphPropertyQueue(graphVertex, null, LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.getKey());

        JSONObject json = JsonSerializer.toJson(graphVertex, workspaceId);
        respondWithJson(response, json);
    }
}
