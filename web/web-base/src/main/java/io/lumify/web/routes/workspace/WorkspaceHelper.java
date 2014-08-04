package io.lumify.web.routes.workspace;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.lumify.core.model.audit.AuditAction;
import io.lumify.core.model.audit.AuditRepository;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.termMention.TermMentionModel;
import io.lumify.core.model.termMention.TermMentionRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
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
    private final WorkQueueRepository workQueueRepository;
    private final Graph graph;

    @Inject
    public WorkspaceHelper(
            final TermMentionRepository termMentionRepository,
            final AuditRepository auditRepository,
            final UserRepository userRepository,
            final WorkQueueRepository workQueueRepository,
            final Graph graph) {
        this.termMentionRepository = termMentionRepository;
        this.auditRepository = auditRepository;
        this.userRepository = userRepository;
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

            auditRepository.auditVertex(AuditAction.UNRESOLVE, vertex.getId(), "", "", user, visibility.getVisibility());
            result.put("success", true);
        }
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
        json.put("vertex", JsonSerializer.toJson(vertex, workspaceId, authorizations));

        workQueueRepository.pushGraphPropertyQueue(vertex, property);

        return json;
    }

    public JSONObject deleteEdge(Edge edge, Vertex sourceVertex, Vertex destVertex, String imageRelationshipLabel, User user, Authorizations authorizations) {
        graph.removeEdge(edge, authorizations);

        if (edge.getLabel().equals(imageRelationshipLabel)) {
            Property entityHasImage = sourceVertex.getProperty(LumifyProperties.ENTITY_HAS_IMAGE_VERTEX_ID.getPropertyName());
            sourceVertex.removeProperty(entityHasImage.getName(), authorizations);
            this.workQueueRepository.pushElementImageQueue(sourceVertex, entityHasImage);
        }

        for (Property rowKeyProperty : destVertex.getProperties(LumifyProperties.ROW_KEY.getPropertyName())) {
            TermMentionModel termMentionModel = termMentionRepository.findByRowKey((String) rowKeyProperty.getValue(), userRepository.getModelUserContext(authorizations, LumifyVisibility.SUPER_USER_VISIBILITY_STRING));
            if (termMentionModel != null && termMentionModel.getMetadata().getEdgeId().equals(edge.getId())) {
                termMentionRepository.delete(termMentionModel.getRowKey());
                workQueueRepository.pushTextUpdated(sourceVertex.getId().toString());
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
