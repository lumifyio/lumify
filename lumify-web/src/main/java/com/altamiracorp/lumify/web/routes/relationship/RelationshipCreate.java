package com.altamiracorp.lumify.web.routes.relationship;

import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.graph.GraphRelationship;
import com.altamiracorp.lumify.core.model.graph.GraphRepository;
import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RelationshipCreate extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(RelationshipCreate.class);

    private final GraphRepository graphRepository;
    private final AuditRepository auditRepository;
    private final OntologyRepository ontologyRepository;

    @Inject
    public RelationshipCreate(final GraphRepository graphRepository,
                              final AuditRepository auditRepository,
                              final OntologyRepository ontologyRepository) {
        this.graphRepository = graphRepository;
        this.auditRepository = auditRepository;
        this.ontologyRepository = ontologyRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        // validate parameters
        final String sourceGraphVertexId = getRequiredParameter(request, "sourceGraphVertexId");
        final String destGraphVertexId = getRequiredParameter(request, "destGraphVertexId");
        final String predicateLabel = getRequiredParameter(request, "predicateLabel");

        User user = getUser(request);
        GraphRelationship relationship = graphRepository.saveRelationship(sourceGraphVertexId, destGraphVertexId, predicateLabel, user);

        String relationshipDisplayName = ontologyRepository.getDisplayNameForLabel(predicateLabel, user);
        GraphVertex destVertex = graphRepository.findVertex(destGraphVertexId, user);
        GraphVertex sourceVertex = graphRepository.findVertex(sourceGraphVertexId, user);

        // TODO: replace second "" when we implement commenting on ui
        auditRepository.auditRelationships(AuditAction.CREATE.toString(), sourceVertex, destVertex, relationshipDisplayName, "", "", user);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Statement created:\n" + relationship.toJson().toString(2));
        }

        respondWithJson(response, relationship.toJson());
    }
}
