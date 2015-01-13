package io.lumify.web.routes.workspace;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.audit.AuditRepository;
import io.lumify.core.model.termMention.TermMentionRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.model.workspace.diff.WorkspaceDiffHelper;
import io.lumify.core.security.VisibilityTranslator;
import io.lumify.core.user.User;
import io.lumify.core.util.GraphUtil;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import io.lumify.web.clientapi.model.*;
import org.json.JSONArray;
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
                Vertex vertex = graph.getVertex(vertexId, FetchHint.ALL_INCLUDING_HIDDEN, authorizations);
                checkNotNull(vertex);
                if (WorkspaceDiffHelper.isPublicDelete(vertex, authorizations)) {
                    LOGGER.debug("un-hiding vertex: %s (workspaceId: %s)", vertex.getId(), workspaceId);
                    // TODO see WorkspaceHelper.deleteVertex for all the other things we need to bring back
                    graph.markVertexVisible(vertex, new Visibility(workspaceId), authorizations);
                    graph.flush();
                    workQueueRepository.broadcastUndoVertexDelete(vertex);
                } else if (GraphUtil.getSandboxStatus(vertex, workspaceId) == SandboxStatus.PUBLIC) {
                    String msg = "Cannot undo a public vertex";
                    LOGGER.warn(msg);
                    data.setErrorMessage(msg);
                    workspaceUndoResponse.addFailure(data);
                } else {
                    workspaceHelper.deleteVertex(vertex, workspaceId, false, authorizations, user);
                    verticesDeleted.put(vertexId);
                    graph.flush();
                    workQueueRepository.broadcastUndoVertex(vertex);
                }
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
                Edge edge = graph.getEdge(relationshipUndoItem.getEdgeId(), FetchHint.ALL_INCLUDING_HIDDEN, authorizations);
                if (edge == null) {
                    continue;
                }
                Vertex sourceVertex = edge.getVertex(Direction.OUT, authorizations);
                Vertex destVertex = edge.getVertex(Direction.IN, authorizations);
                if (sourceVertex == null || destVertex == null) {
                    continue;
                }

                checkNotNull(edge);

                if (WorkspaceDiffHelper.isPublicDelete(edge, authorizations)) {
                    LOGGER.debug("un-hiding edge: %s (workspaceId: %s)", edge.getId(), workspaceId);
                    // TODO see workspaceHelper.deleteEdge for all the other things we need to bring back
                    graph.markEdgeVisible(edge, new Visibility(workspaceId), authorizations);
                    graph.flush();
                    workQueueRepository.broadcastUndoEdgeDelete(edge);
                } else if (GraphUtil.getSandboxStatus(edge, workspaceId) == SandboxStatus.PUBLIC) {
                    String error_msg = "Cannot undo a public edge";
                    LOGGER.warn(error_msg);
                    data.setErrorMessage(error_msg);
                    workspaceUndoResponse.addFailure(data);
                } else {
                    workspaceHelper.deleteEdge(workspaceId, edge, sourceVertex, destVertex, false, user, authorizations);
                    graph.flush();
                    workQueueRepository.broadcastUndoEdge(edge);
                }
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
                Vertex vertex = graph.getVertex(propertyUndoItem.getVertexId(), FetchHint.ALL_INCLUDING_HIDDEN, authorizations);
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

                    if (WorkspaceDiffHelper.isPublicDelete(property, authorizations)) {
                        LOGGER.debug("un-hiding property: %s (workspaceId: %s)", property, workspaceId);
                        vertex.markPropertyVisible(property, new Visibility(workspaceId), authorizations);
                        graph.flush();
                        workQueueRepository.broadcastUndoPropertyDelete(vertex, propertyKey, propertyName);
                    } else if (propertySandboxStatus == SandboxStatus.PUBLIC) {
                        String error_msg = "Cannot undo a public property";
                        LOGGER.warn(error_msg);
                        data.setErrorMessage(error_msg);
                        workspaceUndoResponse.addFailure(data);
                    } else {
                        workspaceHelper.deleteProperty(vertex, property, false, workspaceId, user, authorizations);
                        graph.flush();
                        workQueueRepository.broadcastUndoProperty(vertex, propertyKey, propertyName);
                    }
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

}
