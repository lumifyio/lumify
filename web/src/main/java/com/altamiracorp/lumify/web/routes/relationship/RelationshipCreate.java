package com.altamiracorp.lumify.web.routes.relationship;

import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.model.graph.GraphRelationship;
import com.altamiracorp.lumify.core.model.graph.GraphRepository;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RelationshipCreate extends BaseRequestHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(RelationshipCreate.class);

    private final GraphRepository graphRepository;

    @Inject
    public RelationshipCreate(final GraphRepository repo) {
        graphRepository = repo;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        // validate parameters
        final String sourceGraphVertexId = getRequiredParameter(request, "sourceGraphVertexId");
        final String destGraphVertexId = getRequiredParameter(request, "destGraphVertexId");
        final String predicateLabel = getRequiredParameter(request, "predicateLabel");

        User user = getUser(request);
        GraphRelationship relationship = graphRepository.saveRelationship(sourceGraphVertexId, destGraphVertexId, predicateLabel, user);

        LOGGER.info("Statement created:\n" + relationship.toJson().toString(2));

        respondWithJson(response, relationship.toJson());
    }
}
