package com.altamiracorp.lumify.web.routes.vertex;

import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.GraphUtil;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Authorizations;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Property;
import com.google.inject.Inject;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class VertexProperties extends BaseRequestHandler {
    private final Graph graph;
    private final UserRepository userRepository;

    @Inject
    public VertexProperties(final Graph graph, final UserRepository userRepository) {
        this.graph = graph;
        this.userRepository = userRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String graphVertexId = getAttributeString(request, "graphVertexId");
        User user = getUser(request);
        Authorizations authorizations = userRepository.getAuthorizations(user);

        Iterable<Property> properties = graph.getVertex(graphVertexId, authorizations).getProperties();
        JSONObject propertiesJson = GraphUtil.toJsonProperties(properties);

        JSONObject json = new JSONObject();
        json.put("id", graphVertexId);
        json.put("properties", propertiesJson);

        respondWithJson(response, json);
    }
}
