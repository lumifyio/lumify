package com.altamiracorp.lumify.web.routes.entity;

import com.altamiracorp.lumify.core.model.artifactHighlighting.TermMentionOffsetItem;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.graph.GraphRepository;
import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.model.graph.InMemoryGraphVertex;
import com.altamiracorp.lumify.core.model.ontology.LabelName;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.model.ontology.VertexType;
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

    @Inject
    public EntityTermCreate(
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
        final long mentionStart = getRequiredParameterAsLong(request, "mentionStart");
        final long mentionEnd = getRequiredParameterAsLong(request, "mentionEnd");
        final String sign = getRequiredParameter(request, "sign");
        final String conceptId = getRequiredParameter(request, "conceptId");

        User user = getUser(request);
        TermMentionRowKey termMentionRowKey = new TermMentionRowKey(artifactId, mentionStart, mentionEnd);

        GraphVertex conceptVertex = graphRepository.findVertex(conceptId, user);

        final GraphVertex createdVertex = new InMemoryGraphVertex();
        final GraphVertex artifactVertex = graphRepository.findVertex(artifactId, user);
        createdVertex.setType(VertexType.ENTITY);
        createdVertex.setProperty(PropertyName.ROW_KEY, termMentionRowKey.toString());

        entityHelper.updateGraphVertex(createdVertex, conceptId, sign, user);

        auditRepository.audit(artifactVertex.getId(), auditRepository.createEntityAuditMessage(), user);
        auditRepository.audit(createdVertex.getId(), auditRepository.vertexPropertyAuditMessage(PropertyName.TYPE.toString(), VertexType.ENTITY.toString()), user);
        auditRepository.audit(createdVertex.getId(), auditRepository.vertexPropertyAuditMessage(PropertyName.ROW_KEY.toString(), termMentionRowKey.toString()), user);

        graphRepository.saveRelationship(artifactId, createdVertex.getId(), LabelName.HAS_ENTITY, user);

        TermMention termMention = new TermMention(termMentionRowKey);
        entityHelper.updateTermMention(termMention, sign, conceptVertex, createdVertex, user);

        // Modify the highlighted artifact text in a background thread
        entityHelper.scheduleHighlight(artifactId, user);

        TermMentionOffsetItem offsetItem = new TermMentionOffsetItem(termMention, createdVertex);
        respondWithJson(response, offsetItem.toJson());
    }
}
