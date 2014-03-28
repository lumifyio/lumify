package com.altamiracorp.lumify.web.routes.vertex;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.detectedObjects.DetectedObjectModel;
import com.altamiracorp.lumify.core.model.detectedObjects.DetectedObjectRepository;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.user.UserProvider;
import com.altamiracorp.lumify.core.util.GraphUtil;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Authorizations;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Iterator;

public class VertexProperties extends BaseRequestHandler {
    private final Graph graph;
    private final DetectedObjectRepository detectedObjectRepository;
    private final UserProvider userProvider;

    @Inject
    public VertexProperties(
            final Graph graph,
            final UserRepository userRepository,
            final Configuration configuration,
            final DetectedObjectRepository detectedObjectRepository,
            final UserProvider userProvider) {
        super(userRepository, configuration);
        this.graph = graph;
        this.detectedObjectRepository = detectedObjectRepository;
        this.userProvider = userProvider;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String graphVertexId = getAttributeString(request, "graphVertexId");
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        ModelUserContext modelUserContext = userProvider.getModelUserContext(authorizations, getWorkspaceId(request));
        String workspaceId = getWorkspaceId(request);

        Vertex vertex = graph.getVertex(graphVertexId, authorizations);
        if (vertex == null) {
            respondWithNotFound(response);
            return;
        }
        JSONObject json = GraphUtil.toJson(vertex, workspaceId);

        json.put("detectedObjects", detectedObjectRepository.toJSON(vertex, modelUserContext, authorizations, workspaceId));

        respondWithJson(response, json);
    }
}
