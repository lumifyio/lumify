package io.lumify.web.routes.edge;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.audit.AuditAction;
import io.lumify.core.model.audit.AuditRepository;
import io.lumify.core.model.ontology.OntologyProperty;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.security.VisibilityTranslator;
import io.lumify.core.user.User;
import io.lumify.core.util.GraphUtil;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import org.json.JSONObject;
import org.securegraph.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class SetEdgeProperty extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(SetEdgeProperty.class);

    private final Graph graph;
    private final OntologyRepository ontologyRepository;
    private final AuditRepository auditRepository;
    private VisibilityTranslator visibilityTranslator;
    private final WorkQueueRepository workQueueRepository;

    @Inject
    public SetEdgeProperty(
            final OntologyRepository ontologyRepository,
            final Graph graph,
            final AuditRepository auditRepository,
            final VisibilityTranslator visibilityTranslator,
            final UserRepository userRepository,
            final Configuration configuration,
            final WorkspaceRepository workspaceRepository,
            final WorkQueueRepository workQueueRepository) {
        super(userRepository, workspaceRepository, configuration);
        this.ontologyRepository = ontologyRepository;
        this.graph = graph;
        this.auditRepository = auditRepository;
        this.visibilityTranslator = visibilityTranslator;
        this.workQueueRepository = workQueueRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String edgeId = getRequiredParameter(request, "edgeId");
        final String propertyName = getRequiredParameter(request, "propertyName");
        String propertyKey = getOptionalParameter(request, "propertyKey");
        final String valueStr = getRequiredParameter(request, "value");
        final String visibilitySource = getRequiredParameter(request, "visibilitySource");
        final String justificationText = getOptionalParameter(request, "justificationString");
        final String sourceInfo = getOptionalParameter(request, "sourceInfo");
        final String metadataString = getOptionalParameter(request, "metadata");

        String workspaceId = getActiveWorkspaceId(request);

        final JSONObject sourceJson;
        if (sourceInfo != null) {
            sourceJson = new JSONObject(sourceInfo);
        } else {
            sourceJson = new JSONObject();
        }

        if (propertyKey == null) {
            propertyKey = this.graph.getIdGenerator().nextId();
        }

        Map<String, Object> metadata = GraphUtil.metadataStringToMap(metadataString);

        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);

        if (!graph.isVisibilityValid(new Visibility(visibilitySource), authorizations)) {
            LOGGER.warn("%s is not a valid visibility for %s user", visibilitySource, user.getDisplayName());
            respondWithBadRequest(response, "visibilitySource", getString(request, "visibility.invalid"));
            chain.next(request, response);
            return;
        }

        if (propertyName.equals(LumifyProperties.COMMENT.getPropertyName()) && request.getPathInfo().equals("/edge/property")) {
            throw new LumifyException("Use /edge/comment to save comment properties");
        } else if (request.getPathInfo().equals("/edge/comment") && !propertyName.equals(LumifyProperties.COMMENT.getPropertyName())) {
            throw new LumifyException("Use /edge/property to save non-comment properties");
        }

        OntologyProperty property = ontologyRepository.getPropertyByIRI(propertyName);
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
        Edge edge = graph.getEdge(edgeId, authorizations);
        Object oldValue = edge.getPropertyValue(propertyName, 0);
        GraphUtil.VisibilityAndElementMutation<Edge> setPropertyResult = GraphUtil.setProperty(
                graph,
                edge,
                propertyName,
                propertyKey,
                value,
                metadata,
                visibilitySource,
                workspaceId,
                this.visibilityTranslator,
                justificationText,
                sourceJson,
                user,
                authorizations);
        setPropertyResult.elementMutation.save(authorizations);

        String sourceId = edge.getVertexId(Direction.OUT);
        String destId = edge.getVertexId(Direction.IN);

        // TODO: replace "" when we implement commenting on ui
        auditRepository.auditRelationshipProperty(AuditAction.DELETE, sourceId, destId, propertyKey, propertyName, oldValue, null, edge, "", "",
                user, setPropertyResult.visibility.getVisibility());

        this.workQueueRepository.pushGraphPropertyQueue(edge, null, propertyName, workspaceId, visibilitySource);

        respondWithSuccessJson(response);
    }
}
