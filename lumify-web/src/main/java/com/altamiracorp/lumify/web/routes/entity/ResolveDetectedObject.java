package com.altamiracorp.lumify.web.routes.entity;

import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.detectedObjects.DetectedObjectModel;
import com.altamiracorp.lumify.core.model.detectedObjects.DetectedObjectRepository;
import com.altamiracorp.lumify.core.model.detectedObjects.DetectedObjectRowKey;
import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.LabelName;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.security.VisibilityTranslator;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.GraphUtil;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.*;
import com.google.inject.Inject;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.altamiracorp.lumify.core.model.ontology.OntologyLumifyProperties.CONCEPT_TYPE;
import static com.altamiracorp.lumify.core.model.properties.LumifyProperties.TITLE;

public class ResolveDetectedObject extends BaseRequestHandler {
    private final Graph graph;
    private final EntityHelper entityHelper;
    private final AuditRepository auditRepository;
    private final OntologyRepository ontologyRepository;
    private final UserRepository userRepository;
    private final DetectedObjectRepository detectedObjectRepository;
    private final VisibilityTranslator visibilityTranslator;

    @Inject
    public ResolveDetectedObject(
            final EntityHelper entityHelper,
            final Graph graphRepository,
            final AuditRepository auditRepository,
            final OntologyRepository ontologyRepository,
            final UserRepository userRepository,
            final DetectedObjectRepository detectedObjectRepository,
            final VisibilityTranslator visibilityTranslator) {
        this.entityHelper = entityHelper;
        this.graph = graphRepository;
        this.auditRepository = auditRepository;
        this.ontologyRepository = ontologyRepository;
        this.userRepository = userRepository;
        this.detectedObjectRepository = detectedObjectRepository;
        this.visibilityTranslator = visibilityTranslator;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String artifactId = getRequiredParameter(request, "artifactId");
        final String title = getRequiredParameter(request, "title");
        final String conceptId = getRequiredParameter(request, "conceptId");
        final String visibilitySource = getRequiredParameter(request, "visibilitySource");
        final String rowKey = getOptionalParameter(request, "rowKey");
        String x1 = getRequiredParameter(request, "x1"), x2 = getRequiredParameter(request, "x2"),
                y1 = getRequiredParameter(request, "y1"), y2 = getRequiredParameter(request, "y2");

        User user = getUser(request);
        Authorizations authorizations = userRepository.getAuthorizations(user);
        Visibility visibility = visibilityTranslator.toVisibility(visibilitySource);

        Concept concept = ontologyRepository.getConceptById(conceptId);
        Vertex artifactVertex = graph.getVertex(artifactId, authorizations);

        ElementMutation<Vertex> resolvedVertexMutation;
        DetectedObjectModel detectedObjectModel;
        if (rowKey != null) {
            DetectedObjectRowKey detectedObjectRowKey = new DetectedObjectRowKey(rowKey);
            resolvedVertexMutation = graph.prepareVertex(detectedObjectRowKey.getId(), visibility, authorizations);
            detectedObjectModel = detectedObjectRepository.findByRowKey(rowKey, user.getModelUserContext());
            detectedObjectModel.getMetadata().setResolvedId(detectedObjectRowKey.getId());
            detectedObjectRepository.save(detectedObjectModel);
        } else {
            Object id = graph.getIdGenerator().nextId();
            detectedObjectModel = detectedObjectRepository.saveDetectedObject(artifactId, id, conceptId, Long.getLong(x1), Long.getLong(y1), Long.getLong(x2), Long.getLong(y2), true);
            resolvedVertexMutation = graph.prepareVertex(id, visibility, authorizations);
        }

        JSONObject result = detectedObjectModel.toJson();

        CONCEPT_TYPE.setProperty(resolvedVertexMutation, concept.getId(), visibility);
        TITLE.setProperty(resolvedVertexMutation, title, visibility);

        Vertex resolvedVertex = resolvedVertexMutation.save();
        auditRepository.auditVertexElementMutation(resolvedVertexMutation, resolvedVertex, "", user, visibility);

        result.put("entityVertex", GraphUtil.toJson(resolvedVertex));

        graph.addEdge(artifactVertex, resolvedVertex, LabelName.RAW_CONTAINS_IMAGE_OF_ENTITY.toString(), visibility, authorizations);
        String labelDisplayName = ontologyRepository.getDisplayNameForLabel(LabelName.RAW_CONTAINS_IMAGE_OF_ENTITY.toString());
        // TODO: replace second "" when we implement commenting on ui
        auditRepository.auditRelationship(AuditAction.CREATE, artifactVertex, resolvedVertex, labelDisplayName, "", "", user, visibility);

        // TODO: index the new vertex

        graph.flush();

        respondWithJson(response, result);
    }
}
