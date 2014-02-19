package com.altamiracorp.lumify.web.routes.graph;

import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.GraphUtil;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
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
    public GraphFindPath(final Graph graph) {
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);

        final String sourceGraphVertexId = getRequiredParameter(request, "sourceGraphVertexId");
        final String destGraphVertexId = getRequiredParameter(request, "destGraphVertexId");
        final int hops = Integer.parseInt(getRequiredParameter(request, "hops"));

        Vertex sourceVertex = graph.getVertex(sourceGraphVertexId, user.getAuthorizations());
        if (sourceVertex == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Source vertex not found");
            return;
        }

        Vertex destVertex = graph.getVertex(destGraphVertexId, user.getAuthorizations());
        if (destVertex == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Destination vertex not found");
            return;
        }

        JSONObject resultsJson = new JSONObject();
        JSONArray pathResults = new JSONArray();

        Iterable<Path> paths = graph.findPaths(sourceVertex, destVertex, hops, user.getAuthorizations());
        for (Path path : paths) {
            JSONArray verticesJson = GraphUtil.toJson(graph.getVerticesInOrder(path, user.getAuthorizations()));
            pathResults.put(verticesJson);
        }

        resultsJson.put("paths", pathResults);

        respondWithJson(response, resultsJson);
    }
}

