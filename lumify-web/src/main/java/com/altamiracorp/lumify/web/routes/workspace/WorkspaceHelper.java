package com.altamiracorp.lumify.web.routes.workspace;

import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.detectedObjects.DetectedObjectModel;
import com.altamiracorp.lumify.core.model.detectedObjects.DetectedObjectRepository;
import com.altamiracorp.lumify.core.model.termMention.TermMentionModel;
import com.altamiracorp.lumify.core.model.termMention.TermMentionRepository;
import com.altamiracorp.lumify.core.model.textHighlighting.TermMentionOffsetItem;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.model.workspace.WorkspaceRepository;
import com.altamiracorp.lumify.core.security.LumifyVisibility;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.GraphUtil;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.Messaging;
import com.altamiracorp.securegraph.*;
import com.altamiracorp.securegraph.util.IterableUtils;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;

import static com.altamiracorp.securegraph.util.IterableUtils.toList;

@Singleton
public class WorkspaceHelper {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(WorkspaceHelper.class);
    private final ModelSession modelSession;
    private final TermMentionRepository termMentionRepository;
    private final AuditRepository auditRepository;
    private final UserRepository userRepository;
    private final DetectedObjectRepository detectedObjectRepository;
    private final Graph graph;

    @Inject
    public WorkspaceHelper(final ModelSession modelSession,
                           final TermMentionRepository termMentionRepository,
                           final AuditRepository auditRepository,
                           final UserRepository userRepository,
                           final DetectedObjectRepository detectedObjectRepository,
                           final Graph graph) {
        this.modelSession = modelSession;
        this.termMentionRepository = termMentionRepository;
        this.auditRepository = auditRepository;
        this.userRepository = userRepository;
        this.detectedObjectRepository = detectedObjectRepository;
        this.graph = graph;
    }

    public JSONObject unresolveTerm(Vertex vertex, String edgeId, TermMentionModel termMention, TermMentionModel analyzedTermMention, LumifyVisibility visibility,
                                    ModelUserContext modelUserContext, User user, Authorizations authorizations) {
        JSONObject result = new JSONObject();
        if (termMention == null) {
            LOGGER.warn("invalid term mention row");
        } else {
            Vertex artifactVertex = graph.getVertex(termMention.getRowKey().getGraphVertexId(), authorizations);

            // If there is only instance of the term entity in this artifact delete the relationship
            Iterator<TermMentionModel> termMentionModels = termMentionRepository.findByGraphVertexId(termMention.getRowKey().getGraphVertexId(), modelUserContext).iterator();
            boolean deleteEdge = false;
            int termCount = 0;
            while (termMentionModels.hasNext()) {
                TermMentionModel termMentionModel = termMentionModels.next();
                Object termMentionId = termMentionModel.getMetadata().getGraphVertexId();
                if (termMentionId != null && termMentionId.equals(vertex.getId())) {
                    termCount++;
                    break;
                }
            }
            if (termCount == 1) {
                if (edgeId != null) {
                    Edge edge = graph.getEdge(edgeId, authorizations);
                    graph.removeEdge(edgeId, authorizations);
                    deleteEdge = true;
                    auditRepository.auditRelationship(AuditAction.DELETE, artifactVertex, vertex, edge, "", "", user, visibility.getVisibility());
                }
            }

            modelSession.deleteRow(termMention.getTableName(), termMention.getRowKey());

            if (analyzedTermMention != null) {
                TermMentionOffsetItem offsetItem = new TermMentionOffsetItem(analyzedTermMention);
                result = offsetItem.toJson();
            }

            graph.flush();

            if (deleteEdge) {
                result.put("deleteEdge", deleteEdge);
                result.put("edgeId", edgeId);
            }
        }
        return result;
    }

    public JSONObject unresolveDetectedObject(Vertex vertex, String edgeId, DetectedObjectModel detectedObjectModel,
                                              DetectedObjectModel analyzedDetectedObject,
                                              LumifyVisibility visibility, String workspaceId,
                                              ModelUserContext modelUserContext, User user,
                                              Authorizations authorizations) {
        JSONObject result = new JSONObject();
        Vertex artifactVertex = graph.getVertex(detectedObjectModel.getRowKey().getArtifactId(), authorizations);

        modelSession.deleteRow(detectedObjectModel.getTableName(), detectedObjectModel.getRowKey());
        modelSession.flush();

        if (analyzedDetectedObject == null) {
            result.put("deleteTag", true);
        } else {
            result.put("detectedObject", analyzedDetectedObject.toJson());
        }

        if (edgeId != null) {
            Edge edge = graph.getEdge(edgeId, authorizations);
            graph.removeEdge(edge, authorizations);

            auditRepository.auditRelationship(AuditAction.DELETE, artifactVertex, vertex, edge, "", "", user, visibility.getVisibility());

            result.put("deleteEdge", true);
            result.put("edgeId", edge.getId());
            graph.flush();
        }

        Authorizations systemAuthorization = userRepository.getAuthorizations(user, WorkspaceRepository.VISIBILITY_STRING, workspaceId);
        Vertex workspaceVertex = graph.getVertex(workspaceId, systemAuthorization);
        Iterator<Edge> workspaceToVertex = workspaceVertex.getEdges(vertex, Direction.BOTH, systemAuthorization).iterator();
        while (workspaceToVertex.hasNext()) {
            graph.removeEdge(workspaceToVertex.next(), systemAuthorization);
        }

        JSONObject artifactJson = GraphUtil.toJson(artifactVertex, workspaceId);
        artifactJson.put("detectedObjects", detectedObjectRepository.toJSON(artifactVertex, modelUserContext, authorizations, workspaceId));
        result.put("artifactVertex", artifactJson);
        return result;
    }

    public JSONObject deleteProperty(Vertex vertex, Property property, String workspaceId) {
        vertex.removeProperty(property.getKey(), property.getName());

        graph.flush();

        List<Property> properties = toList(vertex.getProperties(property.getName()));
        JSONObject json = new JSONObject();
        JSONObject propertiesJson = GraphUtil.toJsonProperties(properties, workspaceId);
        json.put("properties", propertiesJson);
        json.put("deletedProperty", property.getName());
        json.put("vertex", GraphUtil.toJson(vertex, workspaceId));

        Messaging.broadcastPropertyChange(vertex.getId().toString(), vertex.getId().toString(), null, json);
        return json;
    }

    public JSONObject deleteEdge(Edge edge, Vertex sourceVertex, Vertex destVertex, User user, Authorizations authorizations) {
        graph.removeEdge(edge, authorizations);

        Messaging.broadcastEdgeDeletion(edge.getId().toString());

        // TODO: replace "" when we implement commenting on ui
        auditRepository.auditRelationship(AuditAction.DELETE, sourceVertex, destVertex, edge, "", "", user, new LumifyVisibility().getVisibility());

        graph.flush();

        JSONObject resultJson = new JSONObject();
        resultJson.put("success", true);
        return resultJson;
    }
}
