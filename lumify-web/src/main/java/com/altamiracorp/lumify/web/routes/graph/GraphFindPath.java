package com.altamiracorp.lumify.web.routes.graph;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.GraphUtil;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Authorizations;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Path;
import com.altamiracorp.securegraph.Vertex;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GraphFindPath extends BaseRequestHandler {
    private final Graph graph;

    @Inject
    public GraphFindPath(
            final Graph graph,
            final UserRepository userRepository,
            final Configuration configuration) {
        super(userRepository, configuration);
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getWorkspaceId(request);

        final String sourceGraphVertexId = getRequiredParameter(request, "sourceGraphVertexId");
        final String destGraphVertexId = getRequiredParameter(request, "destGraphVertexId");
        final int hops = Integer.parseInt(getRequiredParameter(request, "hops"));

        Vertex sourceVertex = graph.getVertex(sourceGraphVertexId, authorizations);
        if (sourceVertex == null) {
            respondWithNotFound(response, "Source vertex not found");
            return;
        }

        Vertex destVertex = graph.getVertex(destGraphVertexId, authorizations);
        if (destVertex == null) {
            respondWithNotFound(response, "Destination vertex not found");
            return;
        }

        JSONObject resultsJson = new JSONObject();
        JSONArray pathResults = new JSONArray();

        Iterable<Path> paths = graph.findPaths(sourceVertex, destVertex, hops, authorizations);
        for (Path path : paths) {
            JSONArray verticesJson = GraphUtil.toJson(graph.getVerticesInOrder(path, authorizations), workspaceId);
            pathResults.put(verticesJson);
        }

        resultsJson.put("paths", pathResults);

        respondWithJson(response, resultsJson);
    }
}

