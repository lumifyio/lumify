package com.altamiracorp.lumify.web.routes.entity;

import com.altamiracorp.lumify.core.ingest.ArtifactDetectedObject;
import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.LabelName;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.*;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.altamiracorp.lumify.core.model.properties.RawLumifyProperties.DETECTED_OBJECTS_JSON;

public class EntityObjectDetectionCreate extends BaseRequestHandler {
    private final Graph graph;
    private final EntityHelper entityHelper;
    private final AuditRepository auditRepository;
    private final OntologyRepository ontologyRepository;
    private final UserRepository userRepository;

    @Inject
    public EntityObjectDetectionCreate(
            final EntityHelper entityHelper,
            final Graph graphRepository,
            final AuditRepository auditRepository,
            final OntologyRepository ontologyRepository,
            final UserRepository userRepository) {
        this.entityHelper = entityHelper;
        this.graph = graphRepository;
        this.auditRepository = auditRepository;
        this.ontologyRepository = ontologyRepository;
        this.userRepository = userRepository;
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
        Authorizations authorizations = userRepository.getAuthorizations(user);

        Concept concept = ontologyRepository.getConceptById(conceptId);
        Vertex artifactVertex = graph.getVertex(artifactId, authorizations);
        ElementMutation<Vertex> artifactVertexMutation = artifactVertex.prepareMutation();

        // create new graph vertex
        // TODO: replace second "" when we implement commenting on ui
        ElementMutation<Vertex> resolvedVertexMutation =
                entityHelper.createGraphMutation(concept, sign, existing, graphVertexId, "", "", authorizations);
        Vertex resolvedVertex = resolvedVertexMutation.save();
        auditRepository.auditVertexElementMutation(resolvedVertexMutation, resolvedVertex, "", user, visibility);

        ArtifactDetectedObject newDetectedObject = entityHelper.createObjectTag(x1, x2, y1, y2, resolvedVertex, concept);

        // adding to detected object property if one exists, if not add detected object property to the artifact vertex
        JSONArray detectedObjectList = new JSONArray();
        String detectedObjects = DETECTED_OBJECTS_JSON.getPropertyValue(artifactVertex);
        if (detectedObjects != null && !detectedObjects.trim().isEmpty()) {
            detectedObjectList = new JSONArray(detectedObjects);
        }

        JSONObject result = new JSONObject();

        JSONObject entityVertex = newDetectedObject.getJson();
        entityVertex.put("artifactId", artifactId);
        detectedObjectList.put(entityVertex);

        DETECTED_OBJECTS_JSON.setProperty(artifactVertexMutation, detectedObjectList.toString(), visibility);
        auditRepository.auditVertexElementMutation(artifactVertexMutation, artifactVertex, "", user, visibility);
        artifactVertex = artifactVertexMutation.save();

        graph.addEdge(artifactVertex, resolvedVertex, LabelName.RAW_CONTAINS_IMAGE_OF_ENTITY.toString(), visibility, authorizations);
        String labelDisplayName = ontologyRepository.getDisplayNameForLabel(LabelName.RAW_CONTAINS_IMAGE_OF_ENTITY.toString());
        // TODO: replace second "" when we implement commenting on ui
        auditRepository.auditRelationship(AuditAction.CREATE, artifactVertex, resolvedVertex, labelDisplayName, "", "", user, visibility);

        result.put("entityVertex", entityVertex);

        JSONObject updatedArtifactVertex =
                entityHelper.formatUpdatedArtifactVertexProperty(artifactId, DETECTED_OBJECTS_JSON.getKey(),
                        DETECTED_OBJECTS_JSON.getPropertyValue(artifactVertex));

        result.put("updatedArtifactVertex", updatedArtifactVertex);

        // TODO: index the new vertex

        graph.flush();

        respondWithJson(response, result);
    }
}
