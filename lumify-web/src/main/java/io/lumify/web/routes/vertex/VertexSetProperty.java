package io.lumify.web.routes.vertex;

import io.lumify.core.config.Configuration;
import io.lumify.core.model.audit.AuditAction;
import io.lumify.core.model.audit.AuditRepository;
import io.lumify.core.model.ontology.OntologyProperty;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.model.workspace.Workspace;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.security.VisibilityTranslator;
import io.lumify.core.user.User;
import io.lumify.core.util.GraphUtil;
import io.lumify.core.util.JsonSerializer;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import org.securegraph.Authorizations;
import org.securegraph.Graph;
import org.securegraph.Vertex;
import org.securegraph.Visibility;
import com.google.inject.Inject;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class VertexSetProperty extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(VertexSetProperty.class);

    private final Graph graph;
    private final OntologyRepository ontologyRepository;
    private final AuditRepository auditRepository;
    private final VisibilityTranslator visibilityTranslator;
    private final WorkspaceRepository workspaceRepository;
    private final WorkQueueRepository workQueueRepository;

    @Inject
    public VertexSetProperty(
            final OntologyRepository ontologyRepository,
            final Graph graph,
            final AuditRepository auditRepository,
            final VisibilityTranslator visibilityTranslator,
            final UserRepository userRepository,
            final Configuration configuration,
            final WorkspaceRepository workspaceRepository,
            final WorkQueueRepository workQueueRepository) {
        super(userRepository, configuration);
        this.ontologyRepository = ontologyRepository;
        this.graph = graph;
        this.auditRepository = auditRepository;
        this.visibilityTranslator = visibilityTranslator;
        this.workspaceRepository = workspaceRepository;
        this.workQueueRepository = workQueueRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String graphVertexId = getAttributeString(request, "graphVertexId");
        final String propertyName = getRequiredParameter(request, "propertyName");
        final String valueStr = getRequiredParameter(request, "value");
        final String visibilitySource = getRequiredParameter(request, "visibilitySource");
        final String justificationText = getOptionalParameter(request, "justificationText");
        final String sourceInfo = getOptionalParameter(request, "sourceInfo");
        User user = getUser(request);

        String workspaceId = getActiveWorkspaceId(request);

        final JSONObject sourceJson;
        if (sourceInfo != null) {
            sourceJson = new JSONObject(sourceInfo);
        } else {
            sourceJson = new JSONObject();
        }

        Authorizations authorizations = getAuthorizations(request, user);

        if (!graph.isVisibilityValid(new Visibility(visibilitySource), authorizations)) {
            LOGGER.warn("%s is not a valid visibility for %s user", visibilitySource, user.getDisplayName());
            respondWithBadRequest(response, "visibilitySource", STRINGS.getString("visibility.invalid"));
            chain.next(request, response);
            return;
        }

        OntologyProperty property = ontologyRepository.getProperty(propertyName);
        if (property == null) {
            throw new RuntimeException("Could not find property: " + propertyName);
        }

        Object value;
        try {
            value = property.convertString(valueStr);
        } catch (Exception ex) {
            LOGGER.warn(String.format("Validation error propertyName: %s, valueStr: %s", propertyName, valueStr), ex);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            return;
        }

        Vertex graphVertex = graph.getVertex(graphVertexId, authorizations);
        GraphUtil.VisibilityAndElementMutation<Vertex> setPropertyResult = GraphUtil.setProperty(
                graphVertex,
                propertyName,
                value,
                visibilitySource,
                workspaceId,
                this.visibilityTranslator,
                justificationText,
                sourceJson,
                user);
        auditRepository.auditVertexElementMutation(AuditAction.UPDATE, setPropertyResult.elementMutation, graphVertex, "", user, setPropertyResult.visibility.getVisibility());
        graphVertex = setPropertyResult.elementMutation.save();
        graph.flush();

        Workspace workspace = workspaceRepository.findById(workspaceId, user);

        this.workspaceRepository.updateEntityOnWorkspace(workspace, graphVertex.getId(), null, null, null, user);

        // TODO: use property key from client when we implement multi-valued properties
        this.workQueueRepository.pushGraphPropertyQueue(graphVertex, null, propertyName);

        JSONObject result = JsonSerializer.toJson(graphVertex, workspaceId);
        respondWithJson(response, result);
    }
}
