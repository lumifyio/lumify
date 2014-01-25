package com.altamiracorp.lumify.web.routes.entity;

import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.*;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

public class EntityRelationships extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(EntityRelationships.class);
    private final Graph graph;

    @Inject
    public EntityRelationships(final Graph graph) {
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);

        long startTime = System.nanoTime();

        String[] ids = request.getParameterValues("ids[]");
        if (ids == null) {
            ids = new String[0];
        }

        List<String> allIds = new ArrayList<String>();
        for (int i = 0; i < ids.length; i++) {
            allIds.add(ids[i]);
        }

        JSONArray resultsJson = new JSONArray();

        Collection<Edge> edges = getAllEdges(allIds, user.getAuthorizations());
        for (Edge edge : edges) {
            JSONObject rel = new JSONObject();
            rel.put("from", edge.getVertexId(Direction.OUT));
            rel.put("to", edge.getVertexId(Direction.IN));
            rel.put("relationshipType", edge.getLabel());
            rel.put("id", edge.getId());
            resultsJson.put(rel);
        }

        long endTime = System.nanoTime();
        LOGGER.debug("Retrieved %d in %dms", edges.size(), (endTime - startTime) / 1000 / 1000);

        respondWithJson(response, resultsJson);
    }

    private Collection<Edge> getAllEdges(List<String> allVertexIds, Authorizations authorizations) {
        Set<Edge> results = new HashSet<Edge>();
        Map<String, Vertex> vertices = toVertices(allVertexIds, authorizations);

        for (Vertex sourceVertex : vertices.values()) {
            for (Vertex destVertex : vertices.values()) {
                Iterable<Edge> edges = sourceVertex.getEdges(destVertex, Direction.BOTH, authorizations);
                for (Edge edge : edges) {
                    results.add(edge);
                }
            }
        }
        return results;
    }

    private Map<String, Vertex> toVertices(List<String> allVertexIds, Authorizations authorizations) {
        Map<String, Vertex> vertices = new HashMap<String, Vertex>();
        for (String vertexId : allVertexIds) {
            vertices.put(vertexId, graph.getVertex(vertexId, authorizations));
        }
        return vertices;
    }
}
