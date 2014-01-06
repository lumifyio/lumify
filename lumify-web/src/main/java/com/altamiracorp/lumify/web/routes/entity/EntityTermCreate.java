package com.altamiracorp.lumify.web.routes.entity;

import com.altamiracorp.lumify.core.model.artifactHighlighting.TermMentionOffsetItem;
import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.graph.GraphRepository;
import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.model.graph.InMemoryGraphVertex;
import com.altamiracorp.lumify.core.model.ontology.LabelName;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.model.termMention.TermMention;
import com.altamiracorp.lumify.core.model.termMention.TermMentionRowKey;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class EntityTermCreate extends BaseRequestHandler {
    private final EntityHelper entityHelper;
    private final GraphRepository graphRepository;
    private final AuditRepository auditRepository;
    private final OntologyRepository ontologyRepository;

    @Inject
    public EntityTermCreate(
            final EntityHelper entityHelper,
            final GraphRepository graphRepository,
            final AuditRepository auditRepository,
            final OntologyRepository ontologyRepository) {
        this.entityHelper = entityHelper;
        this.graphRepository = graphRepository;
        this.auditRepository = auditRepository;
        this.ontologyRepository = ontologyRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        // required parameters
        final String artifactId = getRequiredParameter(request, "artifactId");
        final long mentionStart = getRequiredParameterAsLong(request, "mentionStart");
        final long mentionEnd = getRequiredParameterAsLong(request, "mentionEnd");
        final String sign = getRequiredParameter(request, "sign");
        final String conceptId = getRequiredParameter(request, "conceptId");

        User user = getUser(request);
        TermMentionRowKey termMentionRowKey = new TermMentionRowKey(artifactId, mentionStart, mentionEnd);

        GraphVertex conceptVertex = graphRepository.findVertex(conceptId, user);

        final GraphVertex artifactVertex = graphRepository.findVertex(artifactId, user);
        final GraphVertex createdVertex = new InMemoryGraphVertex();
        createdVertex.setProperty(PropertyName.ROW_KEY, termMentionRowKey.toString());

        // TODO: replace second "" when we implement commenting on ui
        entityHelper.updateGraphVertex(createdVertex, conceptId, sign, "", "", user);

        // TODO: replace second "" when we implement commenting on ui
        auditRepository.auditEntity(AuditAction.CREATE.toString(), createdVertex.getId(), artifactId, sign, conceptId ,"", "", user);
        auditRepository.auditEntityProperties(AuditAction.UPDATE.toString(), createdVertex, PropertyName.ROW_KEY.toString(), "", "", user);

        graphRepository.saveRelationship(artifactId, createdVertex.getId(), LabelName.HAS_ENTITY, user);

        String labelDisplayName = ontologyRepository.getDisplayNameForLabel(LabelName.HAS_ENTITY.toString(), user);
        // TODO: replace second "" when we implement commenting on ui
        auditRepository.auditRelationships(AuditAction.CREATE.toString(), artifactVertex, createdVertex, labelDisplayName, "", "", user);

        TermMention termMention = new TermMention(termMentionRowKey);
        entityHelper.updateTermMention(termMention, sign, conceptVertex, createdVertex, user);

        // Modify the highlighted artifact text in a background thread
        entityHelper.scheduleHighlight(artifactId, user);

        TermMentionOffsetItem offsetItem = new TermMentionOffsetItem(termMention, createdVertex);
        respondWithJson(response, offsetItem.toJson());
    }
}
