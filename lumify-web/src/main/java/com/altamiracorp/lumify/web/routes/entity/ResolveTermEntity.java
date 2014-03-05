package com.altamiracorp.lumify.web.routes.entity;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.LabelName;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.termMention.TermMentionModel;
import com.altamiracorp.lumify.core.model.termMention.TermMentionRepository;
import com.altamiracorp.lumify.core.model.termMention.TermMentionRowKey;
import com.altamiracorp.lumify.core.model.textHighlighting.TermMentionOffsetItem;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.security.LumifyVisibility;
import com.altamiracorp.lumify.core.security.VisibilityTranslator;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.*;
import com.altamiracorp.securegraph.mutation.ElementMutation;
import com.google.inject.Inject;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.altamiracorp.lumify.core.model.ontology.OntologyLumifyProperties.CONCEPT_TYPE;
import static com.altamiracorp.lumify.core.model.properties.LumifyProperties.ROW_KEY;
import static com.altamiracorp.lumify.core.model.properties.LumifyProperties.TITLE;
import static com.altamiracorp.lumify.core.util.CollectionUtil.trySingle;

public class ResolveTermEntity extends BaseRequestHandler {
    private final Graph graph;
    private final AuditRepository auditRepository;
    private final OntologyRepository ontologyRepository;
    private final VisibilityTranslator visibilityTranslator;
    private final TermMentionRepository termMentionRepository;

    @Inject
    public ResolveTermEntity(
            final Graph graphRepository,
            final AuditRepository auditRepository,
            final OntologyRepository ontologyRepository,
            final UserRepository userRepository,
            final VisibilityTranslator visibilityTranslator,
            final Configuration configuration,
            final TermMentionRepository termMentionRepository) {
        super(userRepository, configuration);
        this.graph = graphRepository;
        this.auditRepository = auditRepository;
        this.ontologyRepository = ontologyRepository;
        this.visibilityTranslator = visibilityTranslator;
        this.termMentionRepository = termMentionRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String artifactId = getRequiredParameter(request, "artifactId");
        final long mentionStart = getRequiredParameterAsLong(request, "mentionStart");
        final long mentionEnd = getRequiredParameterAsLong(request, "mentionEnd");
        final String title = getRequiredParameter(request, "sign");
        final String conceptId = getRequiredParameter(request, "conceptId");
        final String visibilitySource = getRequiredParameter(request, "visibilitySource");
        final String graphVertexId = getOptionalParameter(request, "graphVertexId");

        String workspaceId = getWorkspaceId(request);

        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);

        TermMentionRowKey termMentionRowKey = new TermMentionRowKey(artifactId, mentionStart, mentionEnd);

        Concept concept = ontologyRepository.getConceptById(conceptId);

        final Vertex artifactVertex = graph.getVertex(artifactId, authorizations);
        JSONObject visibilityJson = GraphUtil.updateVisibilityJson(null, visibilitySource, workspaceId);
        LumifyVisibility lumifyVisibility = visibilityTranslator.toVisibility(visibilityJson);
        ElementMutation<Vertex> createdVertexMutation;
        if (graphVertexId != null) {
            createdVertexMutation = graph.getVertex(graphVertexId, authorizations).prepareMutation();
        } else {
            createdVertexMutation = graph.prepareVertex(lumifyVisibility.getVisibility(), authorizations);
        }

        ROW_KEY.setProperty(createdVertexMutation, termMentionRowKey.toString(), lumifyVisibility.getVisibility());
        CONCEPT_TYPE.setProperty(createdVertexMutation, conceptId, lumifyVisibility.getVisibility());
        TITLE.setProperty(createdVertexMutation, title, lumifyVisibility.getVisibility());

        Vertex createdVertex = createdVertexMutation.save();

        auditRepository.auditVertexElementMutation(createdVertexMutation, createdVertex, "", user, lumifyVisibility.getVisibility());

        // TODO: a better way to check if the same edge exists instead of looking it up every time?
        Edge edge = trySingle(artifactVertex.getEdges(createdVertex, Direction.OUT, LabelName.RAW_HAS_ENTITY.toString(), authorizations));
        if (edge == null) {
            graph.addEdge(artifactVertex, createdVertex, LabelName.RAW_HAS_ENTITY.toString(), lumifyVisibility.getVisibility(), authorizations);
            String labelDisplayName = ontologyRepository.getDisplayNameForLabel(LabelName.RAW_HAS_ENTITY.toString());
            if (labelDisplayName == null) {
                labelDisplayName = LabelName.RAW_HAS_ENTITY.toString();
            }

            // TODO: replace second "" when we implement commenting on ui
            auditRepository.auditRelationship(AuditAction.CREATE, artifactVertex, createdVertex, labelDisplayName, "", "", user, lumifyVisibility.getVisibility());
        }

        TermMentionModel termMention = new TermMentionModel(termMentionRowKey);
        termMention.getMetadata()
                .setSign(title, lumifyVisibility.getVisibility())
                .setOntologyClassUri(concept.getDisplayName(), lumifyVisibility.getVisibility())
                .setConceptGraphVertexId(concept.getId(), lumifyVisibility.getVisibility())
                .setVertexId(createdVertex.getId().toString(), lumifyVisibility.getVisibility());
        termMentionRepository.save(termMention);

        this.graph.flush();

        TermMentionOffsetItem offsetItem = new TermMentionOffsetItem(termMention, createdVertex);
        respondWithJson(response, offsetItem.toJson());
    }
}
