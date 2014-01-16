package com.altamiracorp.lumify.web.routes.graph;

import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.GraphUtil;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Iterator;
import java.util.List;

public class GraphFindPath extends BaseRequestHandler {
    private final Graph graph;

    @Inject
    public GraphFindPath(final Graph graph) {
        this.graph = graph;
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

        Iterator<List<Object>> verticesIds = graph.findPaths(sourceVertex, destVertex, hops, user.getAuthorizations()).iterator();

        JSONObject resultsJson = new JSONObject();
        JSONArray pathResults = new JSONArray();

        while (verticesIds.hasNext()) {
            List<Object> ids = verticesIds.next();
            JSONArray vertices = new JSONArray();
            for (Object id : ids) {
                vertices.put(GraphUtil.toJson(graph.getVertex(id, user.getAuthorizations())));
            }
            pathResults.put(vertices);
        }

        resultsJson.put("paths", pathResults);

        respondWithJson(response, resultsJson);
    }
}

