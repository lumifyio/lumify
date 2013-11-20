package com.altamiracorp.lumify.web.routes.vertex;

import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.model.graph.GraphRepository;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class VertexRelationshipRemoval extends BaseRequestHandler {
    private final GraphRepository graphRepository;

    @Inject
    public VertexRelationshipRemoval(final GraphRepository repo) {
        graphRepository = repo;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String source = getRequiredParameter(request, "sourceId");
        final String target = getRequiredParameter(request, "targetId");
        final String label = getRequiredParameter(request, "label");

        User user = getUser(request);
        graphRepository.removeRelationship(source, target, label, user);
    }
}
