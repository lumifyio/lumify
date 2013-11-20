package com.altamiracorp.lumify.web.routes.graph;

import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.model.graph.GraphRepository;
import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public class GraphFindPath extends BaseRequestHandler {
    private final GraphRepository graphRepository;

    @Inject
    public GraphFindPath(final GraphRepository repo) {
        graphRepository = repo;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);

        int depth;
        String depthStr = getOptionalParameter(request, "depth");
        if (depthStr == null) {
            depth = 5;
        } else {
            depth = Integer.parseInt(depthStr);
        }

        final String sourceGraphVertexId = getRequiredParameter(request, "sourceGraphVertexId");
        final String destGraphVertexId = getRequiredParameter(request, "destGraphVertexId");
        final Integer hops = Integer.parseInt(getRequiredParameter(request, "hops"));

        GraphVertex sourceVertex = graphRepository.findVertex(sourceGraphVertexId, user);
        if (sourceVertex == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Source vertex not found");
            return;
        }

        GraphVertex destVertex = graphRepository.findVertex(destGraphVertexId, user);
        if (destVertex == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Destination vertex not found");
            return;
        }

        List<List<GraphVertex>> vertices = graphRepository.findPath(sourceVertex, destVertex, depth, hops, user);

        JSONObject resultsJson = new JSONObject();
        resultsJson.put("paths", GraphVertex.toJsonPath(vertices));

        respondWithJson(response, resultsJson);
    }
}

