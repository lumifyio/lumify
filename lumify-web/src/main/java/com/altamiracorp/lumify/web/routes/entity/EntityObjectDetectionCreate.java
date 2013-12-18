package com.altamiracorp.lumify.web.routes.entity;

import com.altamiracorp.lumify.core.ingest.ArtifactDetectedObject;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.graph.GraphRepository;
import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class EntityObjectDetectionCreate extends BaseRequestHandler {
    private final GraphRepository graphRepository;
    private final EntityHelper entityHelper;
    private final AuditRepository auditRepository;

    @Inject
    public EntityObjectDetectionCreate(
            final EntityHelper entityHelper,
            final GraphRepository graphRepository,
            final AuditRepository auditRepository) {
        this.entityHelper = entityHelper;
        this.graphRepository = graphRepository;
        this.auditRepository = auditRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {

        // required parameters
        final String artifactId = getRequiredParameter(request, "artifactId");
        final String sign = getRequiredParameter(request, "sign");
        final String conceptId = getRequiredParameter(request, "conceptId");
        String x1 = getRequiredParameter(request, "x1"), x2 = getRequiredParameter(request, "x2"),
                y1 = getRequiredParameter(request, "y1"), y2 = getRequiredParameter(request, "y2");
        String existing = getOptionalParameter(request, "existing");
        final String boundingBox = "x1: " + x1 + ", y1: " + y1 + ", x2: " + x2 + ", y2: " + y2;

        User user = getUser(request);

        GraphVertex conceptVertex = graphRepository.findVertex(conceptId, user);
        GraphVertex artifactVertex = graphRepository.findVertex(artifactId, user);

        // create new graph vertex
        // TODO: replace second "" when we implement commenting on ui
        GraphVertex resolvedVertex = entityHelper.createGraphVertex(conceptVertex, sign, existing,"", "", artifactId, user);

        ArtifactDetectedObject newDetectedObject = entityHelper.createObjectTag(x1, x2, y1, y2, resolvedVertex, conceptVertex);

        // adding to detected object property if one exists, if not add detected object property to the artifact vertex
        JSONArray detectedObjectList = new JSONArray();
        if (artifactVertex.getPropertyKeys().contains(PropertyName.DETECTED_OBJECTS.toString())) {
            detectedObjectList = new JSONArray(artifactVertex.getProperty(PropertyName.DETECTED_OBJECTS).toString());
        }

        JSONObject result = new JSONObject();

        JSONObject entityVertex = newDetectedObject.getJson();
        entityVertex.put("artifactId", artifactId);
        detectedObjectList.put(entityVertex);
        artifactVertex.setProperty(PropertyName.DETECTED_OBJECTS, detectedObjectList.toString());
        String auditMessage = "Set coordinates from undefined to " + boundingBox;
        auditRepository.audit(artifactId, auditMessage, user);
        graphRepository.saveVertex(resolvedVertex, user);
        result.put("entityVertex", entityVertex);

        JSONObject updatedArtifactVertex =
                entityHelper.formatUpdatedArtifactVertexProperty(artifactId, PropertyName.DETECTED_OBJECTS.toString(), artifactVertex.getProperty(PropertyName.DETECTED_OBJECTS));

        result.put("updatedArtifactVertex", updatedArtifactVertex);

        // TODO: index the new vertex

        respondWithJson(response, result);
    }
}
