package io.lumify.web.routes.vertex;

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
import org.securegraph.*;

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
        final String propertyKey = getOptionalParameter(request, "propertyKey");
        final String valueStr = getOptionalParameter(request, "value");
        final String[] valuesStr = getOptionalParameterArray(request, "value[]");
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

        if (propertyName.equals(LumifyProperties.COMMENT.getPropertyName()) && request.getPathInfo().equals("/vertex/property")) {
            throw new LumifyException("Use /vertex/comment to save comment properties");
        } else if (request.getPathInfo().equals("/vertex/comment") && !propertyName.equals(LumifyProperties.COMMENT.getPropertyName())) {
            throw new LumifyException("Use /vertex/property to save non-comment properties");
        }

        respondWithClientApiObject(response, handle(
                graphVertexId,
                propertyName,
                propertyKey,
                valueStr,
                valuesStr,
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
            String[] valuesStr,
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

        if (valueStr == null && valuesStr != null && valuesStr.length == 1) {
            valueStr = valuesStr[0];
        }
        if (valuesStr == null && valueStr != null) {
            valuesStr = new String[1];
            valuesStr[0] = valueStr;
        }

        Metadata metadata = GraphUtil.metadataStringToMap(metadataString, this.visibilityTranslator.getDefaultVisibility());

        Object value;
        if (propertyName.equals("http://lumify.io#comment")) {
            value = valueStr;
        } else {
            OntologyProperty property = ontologyRepository.getPropertyByIRI(propertyName);
            if (property == null) {
                throw new RuntimeException("Could not find property: " + propertyName);
            }

            if (property.hasDependentPropertyIris()) {
                if (valuesStr == null) {
                    throw new LumifyException("properties with dependent properties must contain a value");
                }
                if (property.getDependentPropertyIris().size() != valuesStr.length) {
                    throw new LumifyException("properties with dependent properties must contain the same number of values. expected " + property.getDependentPropertyIris().size() + " found " + valuesStr.length);
                }

                ClientApiElement clientApiElement = null;
                int valuesIndex = 0;
                for (String dependentPropertyIri : property.getDependentPropertyIris()) {
                    clientApiElement = handle(
                            graphVertexId,
                            dependentPropertyIri,
                            propertyKey,
                            valuesStr[valuesIndex++],
                            null,
                            justificationText,
                            sourceInfo,
                            metadataString,
                            visibilitySource,
                            user,
                            workspaceId,
                            authorizations
                    );
                }
                return clientApiElement;
            } else {
                if (valuesStr != null && valuesStr.length > 1) {
                    throw new LumifyException("properties without dependent properties must not contain more than one value.");
                }
                if (valueStr == null) {
                    throw new LumifyException("properties without dependent properties must have a value");
                }
                try {
                    value = property.convertString(valueStr);
                } catch (Exception ex) {
                    LOGGER.warn(String.format("Validation error propertyName: %s, valueStr: %s", propertyName, valueStr), ex);
                    throw new LumifyException(ex.getMessage(), ex);
                }
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
