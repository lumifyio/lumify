package com.altamiracorp.lumify.web.routes.entity;

import com.altamiracorp.lumify.core.ingest.ArtifactDetectedObject;
import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.Visibility;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class EntityObjectDetectionUpdate extends BaseRequestHandler {
    private final Graph graph;
    private final EntityHelper entityHelper;
    private final AuditRepository auditRepository;
    private final OntologyRepository ontologyRepository;

    @Inject
    public EntityObjectDetectionUpdate(
            final Graph graph,
            final EntityHelper entityHelper,
            final AuditRepository auditRepository,
            final OntologyRepository ontologyRepository) {
        this.graph = graph;
        this.entityHelper = entityHelper;
        this.auditRepository = auditRepository;
        this.ontologyRepository = ontologyRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        //TODO set visibility
        Visibility visibility = new Visibility("");
        User user = getUser(request);
        final String artifactId = getRequiredParameter(request, "artifactId");
        final String sign = getRequiredParameter(request, "sign");
        final String conceptId = getRequiredParameter(request, "conceptId");
        Object resolvedGraphVertexId = getOptionalParameter(request, "graphVertexId");
        String existing = getOptionalParameter(request, "existing");
        String x1 = getRequiredParameter(request, "x1"), x2 = getRequiredParameter(request, "x2"),
                y1 = getRequiredParameter(request, "y1"), y2 = getRequiredParameter(request, "y2");
        final String boundingBox = "x1: " + x1 + ", y1: " + y1 + ", x2: " + x2 + ", y2: " + y2;

        Concept concept = ontologyRepository.getConceptById(conceptId);
        Vertex resolvedVertex;
        if (resolvedGraphVertexId != null) {
            resolvedVertex = graph.getVertex(resolvedGraphVertexId, user.getAuthorizations());
        } else {
            // TODO: replace second "" when we implement commenting on ui
            resolvedVertex = entityHelper.createGraphVertex(concept, sign, existing, resolvedGraphVertexId, "", "", artifactId, user);
            resolvedGraphVertexId = resolvedVertex.getId();
        }
        Vertex artifactVertex = graph.getVertex(artifactId, user.getAuthorizations());

        // update graph vertex
        // TODO: replace second "" when we implement commenting on ui
        entityHelper.updateGraphVertex(resolvedVertex, conceptId, sign, "", "", user);

        // update the detected object property on the artifact
        JSONArray detectedObjects = new JSONArray(artifactVertex.getPropertyValue(PropertyName.DETECTED_OBJECTS.toString(), 0).toString());
        for (int i = 0; i < detectedObjects.length(); i++) {
            JSONObject detectedObject = detectedObjects.getJSONObject(i);
            String oldCoordinates = "x1: " + detectedObject.get("x1") + ", y1: " + detectedObject.get("y1") +
                    ", x2: " + detectedObject.get("x2") + ", y2: " + detectedObject.get("y2");
            if (detectedObject.has("graphVertexId") && detectedObject.get("graphVertexId").equals(resolvedGraphVertexId) ||
                    (oldCoordinates.equals(boundingBox))) {
                ArtifactDetectedObject tag = entityHelper.createObjectTag(x1, x2, y1, y2, resolvedVertex, concept);
                JSONObject result = new JSONObject();

                JSONObject entityTag = tag.getJson();
                entityTag.put("artifactId", artifactId);
                detectedObjects.put(i, entityTag);

                artifactVertex.setProperty(PropertyName.DETECTED_OBJECTS.toString(), detectedObjects.toString(), visibility);
                result.put("entityVertex", entityTag);

                // TODO: replace "" when we implement commenting on ui
                auditRepository.auditEntityProperties(AuditAction.UPDATE.toString(), artifactVertex, PropertyName.DETECTED_OBJECTS.toString(), "", "", user);

                JSONObject updatedArtifactVertex = entityHelper.formatUpdatedArtifactVertexProperty(artifactId, PropertyName.DETECTED_OBJECTS.toString(), artifactVertex.getPropertyValue(PropertyName.DETECTED_OBJECTS.toString(), 0));
                result.put("updatedArtifactVertex", updatedArtifactVertex);

                respondWithJson(response, result);
                break;
            }
        }
    }
}