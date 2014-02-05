package com.altamiracorp.lumify.web.routes.entity;

import static com.altamiracorp.lumify.core.model.properties.LumifyProperties.ROW_KEY;

import com.altamiracorp.lumify.core.model.artifactHighlighting.TermMentionOffsetItem;
import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.LabelName;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.termMention.TermMentionModel;
import com.altamiracorp.lumify.core.model.termMention.TermMentionRowKey;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.ElementMutation;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.Visibility;
import com.google.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class EntityTermCreate extends BaseRequestHandler {
    private final EntityHelper entityHelper;
    private final Graph graph;
    private final AuditRepository auditRepository;
    private final OntologyRepository ontologyRepository;

    @Inject
    public EntityTermCreate(
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
        final long mentionStart = getRequiredParameterAsLong(request, "mentionStart");
        final long mentionEnd = getRequiredParameterAsLong(request, "mentionEnd");
        final String sign = getRequiredParameter(request, "sign");
        final String conceptId = getRequiredParameter(request, "conceptId");

        User user = getUser(request);
        TermMentionRowKey termMentionRowKey = new TermMentionRowKey(artifactId, mentionStart, mentionEnd);

        Concept concept = ontologyRepository.getConceptById(conceptId);

        final Vertex artifactVertex = graph.getVertex(artifactId, user.getAuthorizations());
        ElementMutation<Vertex> createdVertexMutation = graph.prepareVertex(visibility, user.getAuthorizations());
        ROW_KEY.setProperty(createdVertexMutation, termMentionRowKey.toString(), visibility);

        // TODO: replace second "" when we implement commenting on ui
        createdVertexMutation = entityHelper.updateMutation(createdVertexMutation, conceptId, sign, "", "", user);

        Vertex createdVertex = createdVertexMutation.save();

        auditRepository.auditVertexElementMutation(createdVertexMutation, createdVertex, "", user);

        graph.addEdge(createdVertex, artifactVertex, LabelName.RAW_HAS_ENTITY.toString(), visibility, user.getAuthorizations());

        String labelDisplayName = ontologyRepository.getDisplayNameForLabel(LabelName.RAW_HAS_ENTITY.toString());
        if (labelDisplayName == null) {
            labelDisplayName = LabelName.RAW_HAS_ENTITY.toString();
        }

        // TODO: replace second "" when we implement commenting on ui
        auditRepository.auditRelationship(AuditAction.CREATE, artifactVertex, createdVertex, labelDisplayName, "", "", user);

        TermMentionModel termMention = new TermMentionModel(termMentionRowKey);
        entityHelper.updateTermMention(termMention, sign, concept, createdVertex, user);

        this.graph.flush();

        // Modify the highlighted artifact text in a background thread
        entityHelper.scheduleHighlight(artifactId, user);

        TermMentionOffsetItem offsetItem = new TermMentionOffsetItem(termMention, createdVertex);
        respondWithJson(response, offsetItem.toJson());
    }
}
