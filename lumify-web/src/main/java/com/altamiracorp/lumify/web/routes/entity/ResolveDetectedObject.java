package com.altamiracorp.lumify.web.routes.entity;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.detectedObjects.DetectedObjectModel;
import com.altamiracorp.lumify.core.model.detectedObjects.DetectedObjectRepository;
import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.LabelName;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.security.LumifyVisibility;
import com.altamiracorp.lumify.core.security.VisibilityTranslator;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.GraphUtil;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Authorizations;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.mutation.ElementMutation;
import com.google.inject.Inject;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.altamiracorp.lumify.core.model.ontology.OntologyLumifyProperties.CONCEPT_TYPE;
import static com.altamiracorp.lumify.core.model.properties.LumifyProperties.TITLE;

public class ResolveDetectedObject extends BaseRequestHandler {
    private final Graph graph;
    private final AuditRepository auditRepository;
    private final OntologyRepository ontologyRepository;
    private final DetectedObjectRepository detectedObjectRepository;
    private final VisibilityTranslator visibilityTranslator;

    @Inject
    public ResolveDetectedObject(
            final Graph graphRepository,
            final AuditRepository auditRepository,
            final OntologyRepository ontologyRepository,
            final UserRepository userRepository,
            final Configuration configuration,
            final DetectedObjectRepository detectedObjectRepository,
            final VisibilityTranslator visibilityTranslator) {
        super(userRepository, configuration);
        this.graph = graphRepository;
        this.auditRepository = auditRepository;
        this.ontologyRepository = ontologyRepository;
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
        final String graphVertexId = getOptionalParameter(request, "graphVertexId");
        String x1 = getRequiredParameter(request, "x1"), x2 = getRequiredParameter(request, "x2"),
                y1 = getRequiredParameter(request, "y1"), y2 = getRequiredParameter(request, "y2");

        String workspaceId = getWorkspaceId(request);
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        JSONObject visibilityJson = GraphUtil.updateVisibilitySourceAndAddWorkspaceId(null, visibilitySource, workspaceId);
        LumifyVisibility lumifyVisibility = visibilityTranslator.toVisibility(visibilityJson);

        Concept concept = ontologyRepository.getConceptById(conceptId);
        Vertex artifactVertex = graph.getVertex(artifactId, authorizations);
        Vertex resolvedVertex;
        ElementMutation<Vertex> resolvedVertexMutation;
        DetectedObjectModel detectedObjectModel;
        Object id = graphVertexId != null && !graphVertexId.equals("") ? graphVertexId : graph.getIdGenerator().nextId();

        if (rowKey != null) {
            detectedObjectModel = detectedObjectRepository.findByRowKey(rowKey, user.getModelUserContext());
            detectedObjectModel.getMetadata().setResolvedId(id, lumifyVisibility.getVisibility());
            detectedObjectRepository.save(detectedObjectModel);
        } else {
            detectedObjectModel = detectedObjectRepository.saveDetectedObject(artifactId, id, conceptId, Double.parseDouble(x1), Double.parseDouble(y1), Double.parseDouble(x2), Double.parseDouble(y2), true, null, lumifyVisibility.getVisibility());
        }

        if (graphVertexId == null || graphVertexId.equals("")) {
            resolvedVertexMutation = graph.prepareVertex(id, lumifyVisibility.getVisibility(), authorizations);
            CONCEPT_TYPE.setProperty(resolvedVertexMutation, concept.getId(), lumifyVisibility.getVisibility());
            TITLE.setProperty(resolvedVertexMutation, title, lumifyVisibility.getVisibility());

            resolvedVertex = resolvedVertexMutation.save();
            auditRepository.auditVertexElementMutation(resolvedVertexMutation, resolvedVertex, "", user, lumifyVisibility.getVisibility());

        } else {
            resolvedVertex = graph.getVertex(id, authorizations);
        }
        JSONObject result = detectedObjectModel.toJson();
        result.put("entityVertex", GraphUtil.toJson(resolvedVertex));

        graph.addEdge(artifactVertex, resolvedVertex, LabelName.RAW_CONTAINS_IMAGE_OF_ENTITY.toString(), lumifyVisibility.getVisibility(), authorizations);
        String labelDisplayName = ontologyRepository.getDisplayNameForLabel(LabelName.RAW_CONTAINS_IMAGE_OF_ENTITY.toString());
        // TODO: replace second "" when we implement commenting on ui
        auditRepository.auditRelationship(AuditAction.CREATE, artifactVertex, resolvedVertex, labelDisplayName, "", "", user, lumifyVisibility.getVisibility());

        // TODO: index the new vertex

        graph.flush();

        respondWithJson(response, result);
    }
}
