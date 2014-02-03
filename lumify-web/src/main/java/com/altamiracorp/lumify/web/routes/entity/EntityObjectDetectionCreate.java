package com.altamiracorp.lumify.web.routes.entity;

import com.altamiracorp.lumify.core.ingest.ArtifactDetectedObject;
import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.LabelName;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.ElementMutation;
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
    private final OntologyRepository ontologyRepository;

    @Inject
    public EntityObjectDetectionCreate(
            final EntityHelper entityHelper,
            final Graph graphRepository,
            final AuditRepository auditRepository,
            final OntologyRepository ontologyRepository) {
        this.entityHelper = entityHelper;
        this.graph = graphRepository;
        this.auditRepository = auditRepository;
        this.ontologyRepository = ontologyRepository;
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
        String graphVertexId = getOptionalParameter(request, "graphVertexId");

        User user = getUser(request);

        Concept concept = ontologyRepository.getConceptById(conceptId);
        Vertex artifactVertex = graph.getVertex(artifactId, user.getAuthorizations());
        ElementMutation<Vertex> artifactVertexMutation = artifactVertex.prepareMutation();

        // create new graph vertex
        // TODO: replace second "" when we implement commenting on ui
        ElementMutation<Vertex> resolvedVertexMutation = entityHelper.createGraphMutation(concept, sign, existing, graphVertexId, "", "", user);
        Vertex resolvedVertex = resolvedVertexMutation.save();
        auditRepository.auditVertexElementMutation(resolvedVertexMutation, resolvedVertex, "", user);

        ArtifactDetectedObject newDetectedObject = entityHelper.createObjectTag(x1, x2, y1, y2, resolvedVertex, concept);

        // adding to detected object property if one exists, if not add detected object property to the artifact vertex
        JSONArray detectedObjectList = new JSONArray();
        if (artifactVertex.getPropertyValue(PropertyName.DETECTED_OBJECTS.toString(), 0) != null) {
            detectedObjectList = new JSONArray(artifactVertex.getPropertyValue(PropertyName.DETECTED_OBJECTS.toString(), 0).toString());
        }

        JSONObject result = new JSONObject();

        JSONObject entityVertex = newDetectedObject.getJson();
        entityVertex.put("artifactId", artifactId);
        detectedObjectList.put(entityVertex);

        artifactVertexMutation.setProperty(PropertyName.DETECTED_OBJECTS.toString(), detectedObjectList.toString(), visibility);
        auditRepository.auditVertexElementMutation(artifactVertexMutation, artifactVertex, "", user);
        artifactVertex = artifactVertexMutation.save();

        graph.addEdge(artifactVertex, resolvedVertex, LabelName.RAW_CONTAINS_IMAGE_OF_ENTITY.toString(), visibility, user.getAuthorizations());
        String labelDisplayName = ontologyRepository.getDisplayNameForLabel(LabelName.RAW_CONTAINS_IMAGE_OF_ENTITY.toString());
        // TODO: replace second "" when we implement commenting on ui
        auditRepository.auditRelationship(AuditAction.CREATE, artifactVertex, resolvedVertex, labelDisplayName, "", "", user);

        result.put("entityVertex", entityVertex);

        JSONObject updatedArtifactVertex =
                entityHelper.formatUpdatedArtifactVertexProperty(artifactId, PropertyName.DETECTED_OBJECTS.toString(), artifactVertex.getPropertyValue(PropertyName.DETECTED_OBJECTS.toString(), 0));

        result.put("updatedArtifactVertex", updatedArtifactVertex);

        // TODO: index the new vertex

        graph.flush();

        respondWithJson(response, result);
    }
}
