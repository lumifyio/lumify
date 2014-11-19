package io.lumify.web.routes.workspace;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.audit.AuditAction;
import io.lumify.core.model.audit.AuditRepository;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.termMention.TermMentionRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.core.security.VisibilityTranslator;
import io.lumify.core.user.User;
import io.lumify.core.util.GraphUtil;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import io.lumify.web.clientapi.model.ClientApiWorkspaceUndoResponse;
import io.lumify.web.clientapi.model.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.securegraph.*;
import org.securegraph.util.IterableUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class WorkspaceUndo extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(WorkspaceUndo.class);
    private final TermMentionRepository termMentionRepository;
    private final Graph graph;
    private final VisibilityTranslator visibilityTranslator;
    private final UserRepository userRepository;
    private final WorkQueueRepository workQueueRepository;
    private final AuditRepository auditRepository;
    private final WorkspaceHelper workspaceHelper;
    private final String entityHasImageIri;
    private final String artifactContainsImageOfEntityIri;

    @Inject
    public WorkspaceUndo(
            final TermMentionRepository termMentionRepository,
            final Configuration configuration,
            final Graph graph,
            final VisibilityTranslator visibilityTranslator,
            final UserRepository userRepository,
            final WorkspaceHelper workspaceHelper,
            final WorkspaceRepository workspaceRepository,
            final WorkQueueRepository workQueueRepository,
            final AuditRepository auditRepository) {
        super(userRepository, workspaceRepository, configuration);
        this.termMentionRepository = termMentionRepository;
        this.graph = graph;
        this.visibilityTranslator = visibilityTranslator;
        this.workspaceHelper = workspaceHelper;
        this.userRepository = userRepository;
        this.workQueueRepository = workQueueRepository;
        this.auditRepository = auditRepository;

        this.entityHasImageIri = this.getConfiguration().get(Configuration.ONTOLOGY_IRI_ENTITY_HAS_IMAGE);
        if (this.entityHasImageIri == null) {
            throw new LumifyException("Could not find configuration for " + Configuration.ONTOLOGY_IRI_ENTITY_HAS_IMAGE);
        }

        this.artifactContainsImageOfEntityIri = this.getConfiguration().get(Configuration.ONTOLOGY_IRI_ARTIFACT_CONTAINS_IMAGE_OF_ENTITY);
        if (this.artifactContainsImageOfEntityIri == null) {
            throw new LumifyException("Could not find configuration for " + Configuration.ONTOLOGY_IRI_ARTIFACT_CONTAINS_IMAGE_OF_ENTITY);
        }
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String undoDataString = getRequiredParameter(request, "undoData");
        ClientApiUndoItem[] undoData = getObjectMapper().readValue(undoDataString, ClientApiUndoItem[].class);
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getActiveWorkspaceId(request);

        LOGGER.debug("undoing:\n%s", Joiner.on("\n").join(undoData));
        ClientApiWorkspaceUndoResponse workspaceUndoResponse = new ClientApiWorkspaceUndoResponse();
        undoVertices(undoData, workspaceUndoResponse, workspaceId, user, authorizations);
        undoEdges(undoData, workspaceUndoResponse, workspaceId, user, authorizations);
        undoProperties(undoData, workspaceUndoResponse, workspaceId, user, authorizations);
        LOGGER.debug("undoing results: %s", workspaceUndoResponse);
        respondWithClientApiObject(response, workspaceUndoResponse);
    }

    private void undoVertices(ClientApiUndoItem[] undoItem, ClientApiWorkspaceUndoResponse workspaceUndoResponse, String workspaceId, User user, Authorizations authorizations) {
        LOGGER.debug("BEGIN undoVertices");
        JSONArray verticesDeleted = new JSONArray();
        for (ClientApiUndoItem data : undoItem) {
            try {
                if (!(data instanceof ClientApiVertexUndoItem)) {
                    continue;
                }
                ClientApiVertexUndoItem vertexUndoItem = (ClientApiVertexUndoItem) data;
                String vertexId = vertexUndoItem.getVertexId();
                checkNotNull(vertexId);
                Vertex vertex = graph.getVertex(vertexId, authorizations);
                checkNotNull(vertex);
                if (GraphUtil.getSandboxStatus(vertex, workspaceId) == SandboxStatus.PUBLIC) {
                    String msg = "Cannot undo a public vertex";
                    LOGGER.warn(msg);
                    data.setErrorMessage(msg);
                    workspaceUndoResponse.addFailure(data);
                    continue;
                }
                undoVertex(vertex, workspaceId, authorizations, user);
                verticesDeleted.put(vertexId);
            } catch (Exception ex) {
                LOGGER.error("Error undoing %s", data.toString(), ex);
                data.setErrorMessage(ex.getMessage());
                workspaceUndoResponse.addFailure(data);
            }
        }
        LOGGER.debug("END undoVertices");
        if (verticesDeleted.length() > 0) {
            workQueueRepository.pushVerticesDeletion(verticesDeleted);
        }
        graph.flush();
    }

    private void undoEdges(ClientApiUndoItem[] undoItem, ClientApiWorkspaceUndoResponse workspaceUndoResponse, String workspaceId, User user, Authorizations authorizations) {
        LOGGER.debug("BEGIN undoEdges");
        for (ClientApiUndoItem data : undoItem) {
            try {
                if (!(data instanceof ClientApiRelationshipUndoItem)) {
                    continue;
                }

                ClientApiRelationshipUndoItem relationshipUndoItem = (ClientApiRelationshipUndoItem) data;
                Edge edge = graph.getEdge(relationshipUndoItem.getEdgeId(), authorizations);
                if (edge == null) {
                    continue;
                }
                Vertex sourceVertex = edge.getVertex(Direction.OUT, authorizations);
                Vertex destVertex = edge.getVertex(Direction.IN, authorizations);
                if (sourceVertex == null || destVertex == null) {
                    continue;
                }

                checkNotNull(edge);
                if (GraphUtil.getSandboxStatus(edge, workspaceId) == SandboxStatus.PUBLIC) {
                    String error_msg = "Cannot undo a public edge";
                    LOGGER.warn(error_msg);
                    data.setErrorMessage(error_msg);
                    workspaceUndoResponse.addFailure(data);
                    continue;
                }
                workspaceHelper.deleteEdge(edge, sourceVertex, destVertex, entityHasImageIri, user, workspaceId, authorizations);
            } catch (Exception ex) {
                LOGGER.error("Error publishing %s", data.toString(), ex);
                data.setErrorMessage(ex.getMessage());
                workspaceUndoResponse.addFailure(data);
            }
        }
        LOGGER.debug("END undoEdges");
        graph.flush();
    }

    private void undoProperties(ClientApiUndoItem[] undoItem, ClientApiWorkspaceUndoResponse workspaceUndoResponse, String workspaceId, User user, Authorizations authorizations) {
        LOGGER.debug("BEGIN undoProperties");
        for (ClientApiUndoItem data : undoItem) {
            try {
                if (!(data instanceof ClientApiPropertyUndoItem)) {
                    continue;
                }
                ClientApiPropertyUndoItem propertyUndoItem = (ClientApiPropertyUndoItem) data;
                Vertex vertex = graph.getVertex(propertyUndoItem.getVertexId(), authorizations);
                if (vertex == null) {
                    continue;
                }
                String propertyKey = propertyUndoItem.getKey();
                String propertyName = propertyUndoItem.getName();
                String propertyVisibilityString = propertyUndoItem.getVisibilityString();
                List<Property> properties = IterableUtils.toList(vertex.getProperties(propertyKey, propertyName));
                SandboxStatus[] sandboxStatuses = GraphUtil.getPropertySandboxStatuses(properties, workspaceId);
                for (int propertyIndex = 0; propertyIndex < properties.size(); propertyIndex++) {
                    Property property = properties.get(propertyIndex);
                    if (propertyVisibilityString != null &&
                            !property.getVisibility().getVisibilityString().equals(propertyVisibilityString)) {
                        continue;
                    }
                    SandboxStatus propertySandboxStatus = sandboxStatuses[propertyIndex];

                    if (propertySandboxStatus == SandboxStatus.PUBLIC) {
                        String error_msg = "Cannot undo a public property";
                        LOGGER.warn(error_msg);
                        data.setErrorMessage(error_msg);
                        workspaceUndoResponse.addFailure(data);
                        continue;
                    }
                    workspaceHelper.deleteProperty(vertex, property, workspaceId, user, authorizations);
                }
            } catch (Exception ex) {
                LOGGER.error("Error publishing %s", data.toString(), ex);
                data.setErrorMessage(ex.getMessage());
                workspaceUndoResponse.addFailure(data);
            }
        }
        LOGGER.debug("End undoProperties");
        graph.flush();
    }

    private JSONArray undoVertex(Vertex vertex, String workspaceId, Authorizations authorizations, User user) {
        JSONArray unresolved = new JSONArray();
        VisibilityJson visibilityJson = LumifyProperties.VISIBILITY_JSON.getPropertyValue(vertex);
        visibilityJson = GraphUtil.updateVisibilityJsonRemoveFromAllWorkspace(visibilityJson);
        LumifyVisibility lumifyVisibility = visibilityTranslator.toVisibility(visibilityJson);

        for (Edge edge : vertex.getEdges(Direction.BOTH, entityHasImageIri, authorizations)) {
            if (edge.getVertexId(Direction.IN).equals(vertex.getId())) {
                Vertex outVertex = edge.getVertex(Direction.OUT, authorizations);
                Property entityHasImage = outVertex.getProperty(LumifyProperties.ENTITY_IMAGE_VERTEX_ID.getPropertyName());
                outVertex.removeProperty(entityHasImage.getName(), authorizations);
                this.workQueueRepository.pushElementImageQueue(outVertex, entityHasImage);
            }
        }

        for (Edge edge : vertex.getEdges(Direction.BOTH, artifactContainsImageOfEntityIri, authorizations)) {
            for (Property rowKeyProperty : vertex.getProperties(LumifyProperties.ROW_KEY.getPropertyName())) {
                String multiValueKey = rowKeyProperty.getValue().toString();
                if (edge.getVertexId(Direction.IN).equals(vertex.getId())) {
                    Vertex outVertex = edge.getVertex(Direction.OUT, authorizations);
                    // remove property
                    LumifyProperties.DETECTED_OBJECT.removeProperty(outVertex, multiValueKey, authorizations);
                    graph.removeEdge(edge, authorizations);
                    auditRepository.auditRelationship(AuditAction.DELETE, outVertex, vertex, edge, "", "", user, lumifyVisibility.getVisibility());
                    this.workQueueRepository.pushEdgeDeletion(edge, workspaceId);
                    this.workQueueRepository.pushGraphPropertyQueue(outVertex, multiValueKey,
                            LumifyProperties.DETECTED_OBJECT.getPropertyName(), workspaceId, visibilityJson.getSource());
                }
            }
        }

        for (Vertex termMention : termMentionRepository.findResolvedTo(vertex.getId(), authorizations)) {
            workspaceHelper.unresolveTerm(vertex, termMention, lumifyVisibility, user, workspaceId, authorizations);
            JSONObject result = new JSONObject();
            result.put("success", true);
            unresolved.put(result);
        }

        Authorizations systemAuthorization = userRepository.getAuthorizations(user, WorkspaceRepository.VISIBILITY_STRING, workspaceId);
        Vertex workspaceVertex = graph.getVertex(workspaceId, systemAuthorization);
        for (Edge edge : workspaceVertex.getEdges(vertex, Direction.BOTH, systemAuthorization)) {
            graph.removeEdge(edge, systemAuthorization);
        }

        graph.removeVertex(vertex, authorizations);
        graph.flush();
        return unresolved;
    }
}
