package com.altamiracorp.lumify.web.routes.entity;

import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.ontology.LabelName;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Graph;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class EntityObjectDetectionDelete extends BaseRequestHandler {
    private final Graph graph;
    private final EntityHelper entityHelper;
    private final AuditRepository auditRepository;

    @Inject
    public EntityObjectDetectionDelete(
            final Graph graph,
            final EntityHelper entityHelper,
            final AuditRepository auditRepository) {
        this.graph = graph;
        this.entityHelper = entityHelper;
        this.auditRepository = auditRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        JSONObject jsonObject = new JSONObject(getRequiredParameter(request, "objectInfo"));
        User user = getUser(request);

        // Delete just the relationship if vertex has more than one relationship otherwise delete vertex
        String graphVertexId = jsonObject.getString("graphVertexId");
        JSONObject obj = new JSONObject();
        obj.put("entityVertex", graph.findVertex(graphVertexId, user).toJson());
        Map<GraphRelationship, GraphVertex> relationships = graph.getRelationships(graphVertexId, user);
        GraphVertex artifactVertex = graph.findVertex(jsonObject.getString("artifactId"), user);
        if (relationships.size() > 1) {
            String edgeId = artifactVertex.getId() + ">" + graphVertexId + "|" + LabelName.CONTAINS_IMAGE_OF.toString();
            obj.put("edgeId", edgeId);
            graph.removeRelationship(artifactVertex.getId(), graphVertexId, LabelName.CONTAINS_IMAGE_OF.toString(), user);
        } else {
            // TODO: replace "" when we implement commenting on ui
            auditRepository.auditEntity(AuditAction.DELETE.toString(), graphVertexId, artifactVertex.getId(), jsonObject.getString("title"),
                    jsonObject.getString("_conceptType"), "", "", user);
            graph.remove(graphVertexId, user);
            obj.put("remove", true);
        }

        // Delete the appropriate detected object json object from the array and reset the property
        JSONArray detectedObjects = new JSONArray(artifactVertex.getProperty(PropertyName.DETECTED_OBJECTS).toString());
        boolean deleted = false;
        for (int i = 0; i < detectedObjects.length(); i++) {
            JSONObject detectedObject = detectedObjects.getJSONObject(i);
            if (detectedObject.has("graphVertexId") && detectedObject.get("graphVertexId").equals(jsonObject.get("graphVertexId"))) {
                detectedObjects.remove(i);
                deleted = true;
                break;
            }
        }
        if (!deleted) {
            throw new RuntimeException("Tag was not found in the list of detected objects");
        }
        artifactVertex.setProperty(PropertyName.DETECTED_OBJECTS, detectedObjects.toString());
        graph.save(artifactVertex, user);

        // TODO: replace "" when we implement commenting on ui
        auditRepository.auditEntityProperties(AuditAction.UPDATE.toString(), artifactVertex, PropertyName.DETECTED_OBJECTS.toString(), "", "", user);

        JSONObject updatedArtifactVertex = entityHelper.formatUpdatedArtifactVertexProperty(artifactVertex.getId(), PropertyName.DETECTED_OBJECTS.toString(), artifactVertex.getProperty(PropertyName.DETECTED_OBJECTS));
        obj.put("updatedArtifactVertex", updatedArtifactVertex);

        //TODO: Overwrite old ElasticSearch index with new info

        respondWithJson(response, obj);
    }
}
