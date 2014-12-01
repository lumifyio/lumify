package io.lumify.web.routes.vertex;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
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
import io.lumify.core.util.ClientApiConverter;
import io.lumify.core.util.GraphUtil;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import io.lumify.web.clientapi.model.ClientApiElement;
import org.json.JSONObject;
import org.securegraph.Authorizations;
import org.securegraph.Graph;
import org.securegraph.Vertex;
import org.securegraph.Visibility;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

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
        super(userRepository, workspaceRepository, configuration);
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
        String propertyKey = getOptionalParameter(request, "propertyKey");
        final String valueStr = getRequiredParameter(request, "value");
        final String visibilitySource = getRequiredParameter(request, "visibilitySource");
        final String justificationText = getOptionalParameter(request, "justificationText");
        final String sourceInfo = getOptionalParameter(request, "sourceInfo");
        final String metadataString = getOptionalParameter(request, "metadata");
        User user = getUser(request);
        String workspaceId = getActiveWorkspaceId(request);
        Authorizations authorizations = getAuthorizations(request, user);

        if (!graph.isVisibilityValid(new Visibility(visibilitySource), authorizations)) {
            LOGGER.warn("%s is not a valid visibility for %s user", visibilitySource, user.getDisplayName());
            respondWithBadRequest(response, "visibilitySource", getString(request, "visibility.invalid"));
            chain.next(request, response);
            return;
        }

        respondWithClientApiObject(response, handle(
                graphVertexId,
                propertyName,
                propertyKey,
                valueStr,
                justificationText,
                sourceInfo,
                metadataString,
                visibilitySource,
                user,
                workspaceId,
                authorizations));
    }

    private ClientApiElement handle(
            String graphVertexId,
            String propertyName,
            String propertyKey,
            String valueStr,
            String justificationText,
            String sourceInfo,
            String metadataString,
            String visibilitySource,
            User user,
            String workspaceId,
            Authorizations authorizations) {
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

        Object value;
        if (propertyName == "http://lumify.io#comment") {
            value = valueStr;
        } else {
            OntologyProperty property = ontologyRepository.getPropertyByIRI(propertyName);
            if (property == null) {
                throw new RuntimeException("Could not find property: " + propertyName);
            }

            try {
                value = property.convertString(valueStr);
            } catch (Exception ex) {
                LOGGER.warn(String.format("Validation error propertyName: %s, valueStr: %s", propertyName, valueStr), ex);
                throw new LumifyException(ex.getMessage(), ex);
            }
        }

        Vertex graphVertex = graph.getVertex(graphVertexId, authorizations);
        GraphUtil.VisibilityAndElementMutation<Vertex> setPropertyResult = GraphUtil.setProperty(
                graph,
                graphVertex,
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
        auditRepository.auditVertexElementMutation(AuditAction.UPDATE, setPropertyResult.elementMutation, graphVertex, "", user, setPropertyResult.visibility.getVisibility());
        graphVertex = setPropertyResult.elementMutation.save(authorizations);
        graph.flush();

        Workspace workspace = workspaceRepository.findById(workspaceId, user);

        this.workspaceRepository.updateEntityOnWorkspace(workspace, graphVertex.getId(), null, null, user);

        this.workQueueRepository.pushGraphPropertyQueue(graphVertex, propertyKey, propertyName, workspaceId, visibilitySource);

        return ClientApiConverter.toClientApi(graphVertex, workspaceId, authorizations);
    }
}
