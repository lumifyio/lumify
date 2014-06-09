package io.lumify.web.routes.workspace;

import com.altamiracorp.bigtable.model.FlushFlag;
import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.lumify.core.model.audit.AuditAction;
import io.lumify.core.model.audit.AuditRepository;
import io.lumify.core.model.detectedObjects.DetectedObjectModel;
import io.lumify.core.model.detectedObjects.DetectedObjectRepository;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.termMention.TermMentionModel;
import io.lumify.core.model.termMention.TermMentionRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.core.user.User;
import io.lumify.core.util.JsonSerializer;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.securegraph.*;

import java.util.Iterator;
import java.util.List;

import static org.securegraph.util.IterableUtils.toList;

@Singleton
public class WorkspaceHelper {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(WorkspaceHelper.class);
    private final TermMentionRepository termMentionRepository;
    private final AuditRepository auditRepository;
    private final UserRepository userRepository;
    private final DetectedObjectRepository detectedObjectRepository;
    private final WorkQueueRepository workQueueRepository;
    private final Graph graph;

    @Inject
    public WorkspaceHelper(final TermMentionRepository termMentionRepository,
                           final AuditRepository auditRepository,
                           final UserRepository userRepository,
                           final DetectedObjectRepository detectedObjectRepository,
                           final WorkQueueRepository workQueueRepository,
                           final Graph graph) {
        this.termMentionRepository = termMentionRepository;
        this.auditRepository = auditRepository;
        this.userRepository = userRepository;
        this.detectedObjectRepository = detectedObjectRepository;
        this.workQueueRepository = workQueueRepository;
        this.graph = graph;
    }

    public JSONObject unresolveTerm(Vertex vertex, String edgeId, TermMentionModel termMention, LumifyVisibility visibility,
                                    ModelUserContext modelUserContext, User user, Authorizations authorizations) {
        JSONObject result = new JSONObject();
        if (termMention == null) {
            LOGGER.warn("invalid term mention row");
        } else {
            Vertex artifactVertex = graph.getVertex(termMention.getRowKey().getGraphVertexId(), authorizations);

            // If there is only instance of the term entity in this artifact delete the relationship
            Iterator<TermMentionModel> termMentionModels = termMentionRepository.findByGraphVertexIdAndPropertyKey(termMention.getRowKey().getGraphVertexId(), termMention.getRowKey().getPropertyKey(), modelUserContext).iterator();
            int termCount = 0;
            while (termMentionModels.hasNext()) {
                TermMentionModel termMentionModel = termMentionModels.next();
                Object termMentionId = termMentionModel.getRowKey().getRowKey();
                if (termMentionId != null && termMention.getRowKey().getRowKey().equals(termMentionId)) {
                    termCount++;
                    break;
                }
            }
            if (termCount == 1) {
                if (edgeId != null) {
                    Edge edge = graph.getEdge(edgeId, authorizations);
                    graph.removeEdge(edgeId, authorizations);
                    workQueueRepository.pushEdgeDeletion(edge);
                    auditRepository.auditRelationship(AuditAction.DELETE, artifactVertex, vertex, edge, "", "", user, visibility.getVisibility());
                }
            }

            termMentionRepository.delete(termMention.getRowKey());
            workQueueRepository.pushTextUpdated(artifactVertex.getId().toString());

            graph.flush();

            auditRepository.auditVertex(AuditAction.UNRESOLVE, vertex.getId(), "", "", user, FlushFlag.FLUSH, visibility.getVisibility());
            result.put("success", true);
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

        detectedObjectRepository.delete(detectedObjectModel.getRowKey());

        if (analyzedDetectedObject == null) {
            result.put("deleteTag", true);
        } else {
            result.put("detectedObject", analyzedDetectedObject.toJson());
        }

        if (edgeId != null) {
            Edge edge = graph.getEdge(edgeId, authorizations);
            graph.removeEdge(edge, authorizations);

            auditRepository.auditRelationship(AuditAction.DELETE, artifactVertex, vertex, edge, "", "", user, visibility.getVisibility());
            this.workQueueRepository.pushEdgeDeletion(edge);
            graph.flush();
        }

        Authorizations systemAuthorization = userRepository.getAuthorizations(user, WorkspaceRepository.VISIBILITY_STRING, workspaceId);
        Vertex workspaceVertex = graph.getVertex(workspaceId, systemAuthorization);
        for (Edge edge : workspaceVertex.getEdges(vertex, Direction.BOTH, systemAuthorization)) {
            graph.removeEdge(edge, systemAuthorization);
        }

        auditRepository.auditVertex(AuditAction.UNRESOLVE, vertex.getId(), "", "", user, FlushFlag.FLUSH, visibility.getVisibility());

        JSONObject artifactJson = JsonSerializer.toJson(artifactVertex, workspaceId);
        artifactJson.put("detectedObjects", detectedObjectRepository.toJSON(artifactVertex, modelUserContext, authorizations, workspaceId));
        result.put("artifactVertex", artifactJson);
        return result;
    }

    public JSONObject deleteProperty(Vertex vertex, Property property, String workspaceId, User user, Authorizations authorizations) {
        auditRepository.auditEntityProperty(AuditAction.DELETE, vertex.getId(), property.getKey(), property.getName(), property.getValue(), null, "", "", property.getMetadata(), user, property.getVisibility());

        vertex.removeProperty(property.getKey(), property.getName(), authorizations);

        graph.flush();

        List<Property> properties = toList(vertex.getProperties(property.getName()));
        JSONObject json = new JSONObject();
        JSONArray propertiesJson = JsonSerializer.toJsonProperties(properties, workspaceId);
        json.put("properties", propertiesJson);
        json.put("deletedProperty", property.getName());
        json.put("vertex", JsonSerializer.toJson(vertex, workspaceId));

        workQueueRepository.pushGraphPropertyQueue(vertex, property);

        return json;
    }

    public JSONObject deleteEdge(Edge edge, Vertex sourceVertex, Vertex destVertex, User user, Authorizations authorizations) {
        graph.removeEdge(edge, authorizations);

        Iterator<Property> rowKeys = destVertex.getProperties(LumifyProperties.ROW_KEY.getPropertyName()).iterator();
        while (rowKeys.hasNext()) {
            Property rowKeyProperty = rowKeys.next();
            TermMentionModel termMentionModel = termMentionRepository.findByRowKey((String) rowKeyProperty.getValue(), userRepository.getModelUserContext(authorizations, LumifyVisibility.SUPER_USER_VISIBILITY_STRING));
            if (termMentionModel == null) {
                DetectedObjectModel detectedObjectModel = detectedObjectRepository.findByRowKey((String) rowKeyProperty.getValue(), userRepository.getModelUserContext(authorizations, LumifyVisibility.SUPER_USER_VISIBILITY_STRING));
                if (detectedObjectModel == null) {
                    continue;
                } else {
                    detectedObjectRepository.delete(detectedObjectModel.getRowKey());
                }
            } else if (termMentionModel.getMetadata().getEdgeId().equals(edge.getId())) {
                termMentionRepository.delete(termMentionModel.getRowKey());
            }
        }

        this.workQueueRepository.pushEdgeDeletion(edge);

        // TODO: replace "" when we implement commenting on ui
        auditRepository.auditRelationship(AuditAction.DELETE, sourceVertex, destVertex, edge, "", "", user, new LumifyVisibility().getVisibility());

        graph.flush();

        JSONObject resultJson = new JSONObject();
        resultJson.put("success", true);
        return resultJson;
    }
}
