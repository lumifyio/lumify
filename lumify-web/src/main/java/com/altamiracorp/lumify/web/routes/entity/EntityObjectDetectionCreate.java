package com.altamiracorp.lumify.web.routes.entity;

import com.altamiracorp.lumify.core.ingest.ArtifactDetectedObject;
import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
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

public class EntityObjectDetectionCreate extends BaseRequestHandler {
    private final Graph graph;
    private final EntityHelper entityHelper;
    private final AuditRepository auditRepository;

    @Inject
    public EntityObjectDetectionCreate(
            final EntityHelper entityHelper,
            final Graph graphRepository,
            final AuditRepository auditRepository) {
        this.entityHelper = entityHelper;
        this.graph = graphRepository;
        this.auditRepository = auditRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        // TODO set visibility
        Visibility visibility = new Visibility("");

        // required parameters
        final String artifactId = getRequiredParameter(request, "artifactId");
        final String sign = getRequiredParameter(request, "sign");
        final String conceptId = getRequiredParameter(request, "conceptId");
        String x1 = getRequiredParameter(request, "x1"), x2 = getRequiredParameter(request, "x2"),
                y1 = getRequiredParameter(request, "y1"), y2 = getRequiredParameter(request, "y2");
        String existing = getOptionalParameter(request, "existing");
        final String boundingBox = "x1: " + x1 + ", y1: " + y1 + ", x2: " + x2 + ", y2: " + y2;

        User user = getUser(request);

        Vertex conceptVertex = graph.getVertex(conceptId, user.getAuthorizations());
        Vertex artifactVertex = graph.getVertex(artifactId, user.getAuthorizations());

        // create new graph vertex
        // TODO: replace second "" when we implement commenting on ui
        Vertex resolvedVertex = entityHelper.createGraphVertex(conceptVertex, sign, existing,"", "", artifactId, user);

        ArtifactDetectedObject newDetectedObject = entityHelper.createObjectTag(x1, x2, y1, y2, resolvedVertex, conceptVertex);

        // adding to detected object property if one exists, if not add detected object property to the artifact vertex
        JSONArray detectedObjectList = new JSONArray();
        if (artifactVertex.getPropertyValue(PropertyName.DETECTED_OBJECTS.toString(), 0) != null) {
            detectedObjectList = new JSONArray(artifactVertex.getPropertyValue(PropertyName.DETECTED_OBJECTS.toString(), 0));
        }

        JSONObject result = new JSONObject();

        JSONObject entityVertex = newDetectedObject.getJson();
        entityVertex.put("artifactId", artifactId);
        detectedObjectList.put(entityVertex);
        artifactVertex.setProperty(PropertyName.DETECTED_OBJECTS.toString(), detectedObjectList.toString(), visibility);

        // TODO: replace "" when we implement commenting on ui
        auditRepository.auditEntityProperties(AuditAction.UPDATE.toString(), artifactVertex, PropertyName.DETECTED_OBJECTS.toString(), "", "", user);

        result.put("entityVertex", entityVertex);

        JSONObject updatedArtifactVertex =
                entityHelper.formatUpdatedArtifactVertexProperty(artifactId, PropertyName.DETECTED_OBJECTS.toString(), artifactVertex.getPropertyValue(PropertyName.DETECTED_OBJECTS.toString(), 0));

        result.put("updatedArtifactVertex", updatedArtifactVertex);

        // TODO: index the new vertex

        respondWithJson(response, result);
    }
}
