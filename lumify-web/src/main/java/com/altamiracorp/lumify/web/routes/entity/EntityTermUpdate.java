package com.altamiracorp.lumify.web.routes.entity;

import com.altamiracorp.lumify.core.model.artifactHighlighting.TermMentionOffsetItem;
import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.graph.GraphRepository;
import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.model.ontology.LabelName;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.model.termMention.TermMentionModel;
import com.altamiracorp.lumify.core.model.termMention.TermMentionRepository;
import com.altamiracorp.lumify.core.model.termMention.TermMentionRowKey;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class EntityTermUpdate extends BaseRequestHandler {
    private final TermMentionRepository termMentionRepository;
    private final GraphRepository graphRepository;
    private final EntityHelper entityHelper;
    private final OntologyRepository ontologyRepository;
    private final AuditRepository auditRepository;

    @Inject
    public EntityTermUpdate(
            final TermMentionRepository termMentionRepository,
            final GraphRepository graphRepository,
            final EntityHelper entityHelper,
            final OntologyRepository ontologyRepository,
            final AuditRepository auditRepository) {
        this.termMentionRepository = termMentionRepository;
        this.graphRepository = graphRepository;
        this.entityHelper = entityHelper;
        this.ontologyRepository = ontologyRepository;
        this.auditRepository = auditRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        // required parameters
        final String artifactId = getRequiredParameter(request, "artifactId");
        final long mentionStart = getRequiredParameterAsLong(request, "mentionStart");
        final long mentionEnd = getRequiredParameterAsLong(request, "mentionEnd");
        final String sign = getRequiredParameter(request, "sign");
        final String conceptId = getRequiredParameter(request, "conceptId");
        final String resolvedGraphVertexId = getRequiredParameter(request, "graphVertexId");

        User user = getUser(request);
        GraphVertex conceptVertex = graphRepository.findVertex(conceptId, user);
        GraphVertex resolvedVertex = graphRepository.findVertex(resolvedGraphVertexId, user);

        // TODO: replace second "" when we implement commenting on ui
        entityHelper.updateGraphVertex(resolvedVertex, conceptId, sign, "", "", user);

        if (graphRepository.findEdge(artifactId, resolvedGraphVertexId, LabelName.HAS_ENTITY.toString(), user) == null) {
            graphRepository.saveRelationship(artifactId, resolvedVertex.getId(), LabelName.HAS_ENTITY, user);
            String labelDisplayName = ontologyRepository.getDisplayNameForLabel(LabelName.HAS_ENTITY.toString(), user);
            GraphVertex artifactVertex = graphRepository.findVertex(artifactId, user);
            // TODO: replace second "" when we implement commenting on ui
            auditRepository.auditRelationships(AuditAction.CREATE.toString(), artifactVertex, resolvedVertex, labelDisplayName, "", "", user);
        }

        TermMentionRowKey termMentionRowKey = new TermMentionRowKey(artifactId, mentionStart, mentionEnd);
        TermMentionModel termMention = termMentionRepository.findByRowKey(termMentionRowKey.toString(), user.getModelUserContext());
        if (termMention == null) {
            termMention = new TermMentionModel(termMentionRowKey);
        }
        entityHelper.updateTermMention(termMention, sign, conceptVertex, resolvedVertex, user);

        entityHelper.scheduleHighlight(artifactId, user);

        TermMentionOffsetItem offsetItem = new TermMentionOffsetItem(termMention, resolvedVertex);
        respondWithJson(response, offsetItem.toJson());
    }
}
