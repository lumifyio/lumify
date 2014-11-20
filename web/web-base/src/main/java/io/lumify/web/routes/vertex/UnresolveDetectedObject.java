package io.lumify.web.routes.vertex;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.ingest.ArtifactDetectedObject;
import io.lumify.core.model.audit.AuditAction;
import io.lumify.core.model.audit.AuditRepository;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.core.security.VisibilityTranslator;
import io.lumify.core.user.User;
import io.lumify.core.util.ClientApiConverter;
import io.lumify.core.util.GraphUtil;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import io.lumify.web.clientapi.model.ClientApiElement;
import io.lumify.web.clientapi.model.SandboxStatus;
import io.lumify.web.clientapi.model.VisibilityJson;
import io.lumify.web.routes.workspace.WorkspaceHelper;
import org.securegraph.Authorizations;
import org.securegraph.Edge;
import org.securegraph.Graph;
import org.securegraph.Vertex;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UnresolveDetectedObject extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(UnresolveDetectedObject.class);
    private final Graph graph;
    private final VisibilityTranslator visibilityTranslator;
    private final AuditRepository auditRepository;
    private final WorkQueueRepository workQueueRepository;

    @Inject
    public UnresolveDetectedObject(
            final Graph graph,
            final UserRepository userRepository,
            final VisibilityTranslator visibilityTranslator,
            final Configuration configuration,
            final WorkspaceRepository workspaceRepository,
            final WorkspaceHelper workspaceHelper,
            AuditRepository auditRepository,
            WorkQueueRepository workQueueRepository) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
        this.visibilityTranslator = visibilityTranslator;
        this.auditRepository = auditRepository;
        this.workQueueRepository = workQueueRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String vertexId = getRequiredParameter(request, "vertexId");
        final String multiValueKey = getRequiredParameter(request, "multiValueKey");
        String workspaceId = getActiveWorkspaceId(request);
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);

        Vertex artifactVertex = graph.getVertex(vertexId, authorizations);
        ArtifactDetectedObject artifactDetectedObject = LumifyProperties.DETECTED_OBJECT.getPropertyValue(artifactVertex, multiValueKey);
        Edge edge = graph.getEdge(artifactDetectedObject.getEdgeId(), authorizations);
        Vertex resolvedVertex = edge.getOtherVertex(artifactVertex.getId(), authorizations);

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

        // remove edge
        graph.removeEdge(edge, authorizations);
        auditRepository.auditRelationship(AuditAction.DELETE, artifactVertex, resolvedVertex, edge, "", "", user, lumifyVisibility.getVisibility());

        // remove property
        LumifyProperties.DETECTED_OBJECT.removeProperty(artifactVertex, multiValueKey, authorizations);

        graph.flush();

        this.workQueueRepository.pushEdgeDeletion(edge);
        this.workQueueRepository.pushGraphPropertyQueue(artifactVertex, multiValueKey,
                LumifyProperties.DETECTED_OBJECT.getPropertyName(), workspaceId, visibilityJson.getSource());

        auditRepository.auditVertex(AuditAction.UNRESOLVE, resolvedVertex.getId(), "", "", user, lumifyVisibility.getVisibility());

        ClientApiElement result = ClientApiConverter.toClientApi(artifactVertex, workspaceId, authorizations);
        respondWithClientApiObject(response, result);
    }
}
