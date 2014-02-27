package com.altamiracorp.lumify.web.routes.entity;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.config.SandboxLevel;
import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.LabelName;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.termMention.TermMentionModel;
import com.altamiracorp.lumify.core.model.termMention.TermMentionRowKey;
import com.altamiracorp.lumify.core.model.textHighlighting.TermMentionOffsetItem;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.security.VisibilityTranslator;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.*;
import com.google.inject.Inject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.altamiracorp.lumify.core.model.properties.LumifyProperties.ROW_KEY;

public class EntityTermCreate extends BaseRequestHandler {
    private final EntityHelper entityHelper;
    private final Graph graph;
    private final AuditRepository auditRepository;
    private final OntologyRepository ontologyRepository;
    private final UserRepository userRepository;
    private final VisibilityTranslator visibilityTranslator;
    private final Configuration configuration;

    @Inject
    public EntityTermCreate(
            final EntityHelper entityHelper,
            final Graph graphRepository,
            final AuditRepository auditRepository,
            final OntologyRepository ontologyRepository,
            final UserRepository userRepository,
            final VisibilityTranslator visibilityTranslator,
            final Configuration configuration) {
        this.entityHelper = entityHelper;
        this.graph = graphRepository;
        this.auditRepository = auditRepository;
        this.ontologyRepository = ontologyRepository;
        this.userRepository = userRepository;
        this.visibilityTranslator = visibilityTranslator;
        this.configuration = configuration;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String artifactId = getRequiredParameter(request, "artifactId");
        final long mentionStart = getRequiredParameterAsLong(request, "mentionStart");
        final long mentionEnd = getRequiredParameterAsLong(request, "mentionEnd");
        final String sign = getRequiredParameter(request, "sign");
        final String conceptId = getRequiredParameter(request, "conceptId");
        final String visibilitySource = getOptionalParameter(request, "visibilitySource");

        String workspaceId;
        if (this.configuration.getSandboxLevel() == SandboxLevel.WORKSPACE) {
            workspaceId = getWorkspaceId(request);
        } else {
            workspaceId = null;
        }

        User user = getUser(request);
        Authorizations authorizations = userRepository.getAuthorizations(user);

        TermMentionRowKey termMentionRowKey = new TermMentionRowKey(artifactId, mentionStart, mentionEnd);

        Concept concept = ontologyRepository.getConceptById(conceptId);

        final Vertex artifactVertex = graph.getVertex(artifactId, authorizations);
        Visibility visibility = visibilityTranslator.toVisibilityWithWorkspace(visibilitySource == null ? "" : visibilitySource, workspaceId);
        ElementMutation<Vertex> createdVertexMutation = graph.prepareVertex(visibility, authorizations);
        ROW_KEY.setProperty(createdVertexMutation, termMentionRowKey.toString(), visibility);

        // TODO: replace second "" when we implement commenting on ui
        createdVertexMutation = entityHelper.updateMutation(createdVertexMutation, conceptId, sign, "", "", user);

        Vertex createdVertex = createdVertexMutation.save();

        auditRepository.auditVertexElementMutation(createdVertexMutation, createdVertex, "", user, visibility);

        graph.addEdge(createdVertex, artifactVertex, LabelName.RAW_HAS_ENTITY.toString(), visibility, authorizations);

        String labelDisplayName = ontologyRepository.getDisplayNameForLabel(LabelName.RAW_HAS_ENTITY.toString());
        if (labelDisplayName == null) {
            labelDisplayName = LabelName.RAW_HAS_ENTITY.toString();
        }

        // TODO: replace second "" when we implement commenting on ui
        auditRepository.auditRelationship(AuditAction.CREATE, artifactVertex, createdVertex, labelDisplayName, "", "", user, visibility);

        TermMentionModel termMention = new TermMentionModel(termMentionRowKey);
        entityHelper.updateTermMention(termMention, sign, concept, createdVertex, visibility, user);

        this.graph.flush();

        TermMentionOffsetItem offsetItem = new TermMentionOffsetItem(termMention, createdVertex);
        respondWithJson(response, offsetItem.toJson());
    }
}
