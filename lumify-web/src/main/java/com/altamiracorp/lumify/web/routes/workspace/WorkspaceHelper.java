package com.altamiracorp.lumify.web.routes.workspace;

import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.detectedObjects.DetectedObjectMetadata;
import com.altamiracorp.lumify.core.model.detectedObjects.DetectedObjectModel;
import com.altamiracorp.lumify.core.model.detectedObjects.DetectedObjectRepository;
import com.altamiracorp.lumify.core.model.ontology.LabelName;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.termMention.TermMentionModel;
import com.altamiracorp.lumify.core.model.termMention.TermMentionRepository;
import com.altamiracorp.lumify.core.model.textHighlighting.TermMentionOffsetItem;
import com.altamiracorp.lumify.core.security.LumifyVisibility;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.GraphUtil;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.securegraph.*;
import com.altamiracorp.securegraph.util.IterableUtils;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;

@Singleton
public class WorkspaceHelper {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(WorkspaceHelper.class);
    private final ModelSession modelSession;
    private final TermMentionRepository termMentionRepository;
    private final AuditRepository auditRepository;
    private final OntologyRepository ontologyRepository;
    private final DetectedObjectRepository detectedObjectRepository;
    private final Graph graph;

    @Inject
    public WorkspaceHelper(final ModelSession modelSession,
                           final TermMentionRepository termMentionRepository,
                           final AuditRepository auditRepository,
                           final OntologyRepository ontologyRepository,
                           final DetectedObjectRepository detectedObjectRepository,
                           final Graph graph) {
        this.modelSession = modelSession;
        this.termMentionRepository = termMentionRepository;
        this.auditRepository = auditRepository;
        this.ontologyRepository = ontologyRepository;
        this.detectedObjectRepository = detectedObjectRepository;
        this.graph = graph;
    }

    public JSONObject unresolveTerm(Vertex vertex, TermMentionModel termMention, LumifyVisibility visibility,
                                    ModelUserContext modelUserContext, User user, Authorizations authorizations,
                                    boolean isPublished) {
        JSONObject result = new JSONObject();
        if (termMention == null) {
            LOGGER.warn("invalid term mention row");
        } else {
            Vertex artifactVertex = graph.getVertex(termMention.getRowKey().getGraphVertexId(), authorizations);
            // Clean up term mentions if system analytics wasn't performed on term
            String columnFamilyName = termMention.getMetadata().getColumnFamilyName();
            String columnName = termMention.getMetadata().VERTEX_ID;
            String analyticProcess = termMention.getMetadata().getAnalyticProcess();

            if (analyticProcess == null) {
                modelSession.deleteRow(termMention.getTableName(), termMention.getRowKey());
            } else {
                termMention.get(columnFamilyName).getColumn(columnName).setDirty(true);
                modelSession.deleteColumn(termMention, termMention.getTableName(), columnFamilyName, columnName);
                termMention.getMetadata().setVertexId("", visibility.getVisibility());

                TermMentionOffsetItem offsetItem = new TermMentionOffsetItem(termMention);
                result = offsetItem.toJson();
            }

            // If there is only instance of the term entity in this artifact delete the relationship
            Iterator<TermMentionModel> termMentionModels = termMentionRepository.findByGraphVertexId(termMention.getRowKey().getGraphVertexId(), modelUserContext).iterator();
            boolean deleteEdge = false;
            Object edgeId = null;
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
                Iterable<Edge> edges = artifactVertex.getEdges(vertex, Direction.OUT, LabelName.RAW_HAS_ENTITY.toString(), authorizations);
                if (edges.iterator().hasNext()) {
                    Edge edge = edges.iterator().next();
                    if (edge != null) {
                        String label = ontologyRepository.getDisplayNameForLabel(edge.getLabel());
                        edgeId = edge.getId();
                        graph.removeEdge(edge, authorizations);
                        deleteEdge = true;
                        auditRepository.auditRelationship(AuditAction.DELETE, artifactVertex, vertex, label, "", "", user, isPublished, visibility.getVisibility());
                    }
                }
            }

            graph.flush();

            if (deleteEdge) {
                result.put("deleteEdge", deleteEdge);
                result.put("edgeId", edgeId);
            }
        }
        return result;
    }

    public JSONObject unresolveDetectedObject(Vertex vertex, DetectedObjectModel detectedObjectModel,
                                              LumifyVisibility visibility, String workspaceId,
                                              ModelUserContext modelUserContext, User user,
                                              Authorizations authorizations, boolean isPublished) {
        JSONObject result = new JSONObject();
        Vertex artifactVertex = graph.getVertex(detectedObjectModel.getRowKey().getArtifactId(), authorizations);
        String columnFamilyName = detectedObjectModel.getMetadata().getColumnFamilyName();
        String columnName = DetectedObjectMetadata.RESOLVED_ID;

        if (detectedObjectModel.getMetadata().getProcess() == null) {
            modelSession.deleteRow(detectedObjectModel.getTableName(), detectedObjectModel.getRowKey());
            result.put("deleteTag", true);
        } else {
            detectedObjectModel.get(columnFamilyName).getColumn(columnName).setDirty(true);
            modelSession.deleteColumn(detectedObjectModel, detectedObjectModel.getTableName(), columnFamilyName, columnName);
            result = detectedObjectModel.toJson();
        }

        Iterable<Object> edgeIds = artifactVertex.getEdgeIds(vertex, Direction.BOTH, authorizations);
        if (IterableUtils.count(edgeIds) == 1) {
            Edge edge = graph.getEdge(edgeIds.iterator().next(), authorizations);
            graph.removeEdge(edge, authorizations);
            String label = ontologyRepository.getDisplayNameForLabel(edge.getLabel());

            auditRepository.auditRelationship(AuditAction.DELETE, artifactVertex, vertex, label, "", "", user, isPublished, visibility.getVisibility());

            result.put("deleteEdge", true);
            result.put("edgeId", edge.getId());
            graph.flush();
        }

        JSONObject artifactJson = GraphUtil.toJson(artifactVertex, workspaceId);
        Iterator<DetectedObjectModel> detectedObjectModels =
                detectedObjectRepository.findByGraphVertexId(artifactVertex.getId().toString(), modelUserContext).iterator();
        JSONArray detectedObjects = new JSONArray();
        while (detectedObjectModels.hasNext()) {
            DetectedObjectModel model = detectedObjectModels.next();
            JSONObject detectedObjectModelJson = model.toJson();
            if (model.getMetadata().getResolvedId() != null) {
                detectedObjectModelJson.put("entityVertex", GraphUtil.toJson(graph.getVertex(model.getMetadata().getResolvedId(), authorizations), workspaceId));
            }
            detectedObjects.put(detectedObjectModelJson);
        }
        artifactJson.put("detectedObjects", detectedObjects);
        result.put("artifactVertex", artifactJson);
        return result;
    }

    public JSONObject deleteProperty (Vertex vertex, List<Property> properties, Property property, String workspaceId) {
        vertex.removeProperty(property.getKey(), property.getName());

        graph.flush();

        // TODO: broadcast property delete
        JSONObject json = new JSONObject();
        JSONObject propertiesJson = GraphUtil.toJsonProperties(properties, workspaceId);
        json.put("properties", propertiesJson);
        json.put("deletedProperty", property.getName());
        json.put("vertex", GraphUtil.toJson(vertex, workspaceId));
        return json;
    }

    public JSONObject deleteEdge (Edge edge, Vertex sourceVertex, Vertex destVertex, User user, Authorizations authorizations,
                                  boolean isPublished) {
        graph.removeEdge(edge, authorizations);

        String displayName = ontologyRepository.getDisplayNameForLabel(edge.getLabel());
        // TODO: replace "" when we implement commenting on ui
        auditRepository.auditRelationship(AuditAction.DELETE, sourceVertex, destVertex, displayName, "", "", user,isPublished, new LumifyVisibility().getVisibility());

        graph.flush();

        JSONObject resultJson = new JSONObject();
        resultJson.put("success", true);
        return resultJson;
    }
}
