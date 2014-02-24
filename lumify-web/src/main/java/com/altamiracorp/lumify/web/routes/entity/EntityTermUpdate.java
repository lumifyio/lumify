package com.altamiracorp.lumify.web.routes.entity;

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
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.*;
import com.google.inject.Inject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Iterator;

public class EntityTermUpdate extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(EntityTermUpdate.class);
    private final TermMentionRepository termMentionRepository;
    private final Graph graph;
    private final EntityHelper entityHelper;
    private final OntologyRepository ontologyRepository;
    private final AuditRepository auditRepository;
    private final UserRepository userRepository;

    @Inject
    public EntityTermUpdate(
            final TermMentionRepository termMentionRepository,
            final Graph graph,
            final EntityHelper entityHelper,
            final OntologyRepository ontologyRepository,
            final AuditRepository auditRepository,
            final UserRepository userRepository) {
        this.termMentionRepository = termMentionRepository;
        this.graph = graph;
        this.entityHelper = entityHelper;
        this.ontologyRepository = ontologyRepository;
        this.auditRepository = auditRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        // required parameters
        final String artifactId = getRequiredParameter(request, "artifactId");
        final long mentionStart = getRequiredParameterAsLong(request, "mentionStart");
        final long mentionEnd = getRequiredParameterAsLong(request, "mentionEnd");
        final String sign = getRequiredParameter(request, "sign");
        final String conceptId = getRequiredParameter(request, "conceptId");
        final String graphVertexId = getRequiredParameter(request, "graphVertexId");

        LOGGER.debug(
                "EntityTermUpdate (artifactId: %s, mentionStart: %d, mentionEnd: %d, sign: %s, conceptId: %s, graphVertexId: %s)",
                artifactId,
                mentionStart,
                mentionEnd,
                sign,
                conceptId,
                graphVertexId);

        User user = getUser(request);
        Authorizations authorizations = userRepository.getAuthorizations(user);

        Concept concept = ontologyRepository.getConceptById(conceptId);
        Vertex resolvedVertex = graph.getVertex(graphVertexId, authorizations);
        ElementMutation<Vertex> resolvedVertexMutation = resolvedVertex.prepareMutation();

        // TODO: replace second "" when we implement commenting on ui
        resolvedVertexMutation = entityHelper.updateMutation(resolvedVertexMutation, conceptId, sign, "", "", user);
        auditRepository.auditVertexElementMutation(resolvedVertexMutation, resolvedVertex, "", user, new Visibility(""));
        resolvedVertex = resolvedVertexMutation.save();

        Vertex artifactVertex = graph.getVertex(artifactId, authorizations);
        Iterator<Edge> edges = artifactVertex.getEdges(resolvedVertex, Direction.BOTH, LabelName.RAW_HAS_ENTITY.toString(), authorizations).iterator();
        if (!edges.hasNext()) {
            graph.addEdge(artifactVertex, resolvedVertex, LabelName.RAW_HAS_ENTITY.toString(), new Visibility(""), authorizations);
            String labelDisplayName = ontologyRepository.getDisplayNameForLabel(LabelName.RAW_HAS_ENTITY.toString());
            // TODO: replace second "" when we implement commenting on ui
            auditRepository.auditRelationship(AuditAction.CREATE, artifactVertex, resolvedVertex, labelDisplayName, "", "", user, new Visibility(""));
        }

        TermMentionRowKey termMentionRowKey = new TermMentionRowKey(artifactId, mentionStart, mentionEnd);
        TermMentionModel termMention = termMentionRepository.findByRowKey(termMentionRowKey.toString(), user.getModelUserContext());
        if (termMention == null) {
            termMention = new TermMentionModel(termMentionRowKey);
        }
        entityHelper.updateTermMention(termMention, sign, concept, resolvedVertex, user);

        this.graph.flush();

        entityHelper.scheduleHighlight(artifactId, user);

        TermMentionOffsetItem offsetItem = new TermMentionOffsetItem(termMention, resolvedVertex);
        respondWithJson(response, offsetItem.toJson());
    }
}
