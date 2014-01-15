package com.altamiracorp.lumify.web.routes.entity;

import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Direction;
import com.altamiracorp.securegraph.Edge;
import com.altamiracorp.securegraph.Graph;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

public class EntityRelationships extends BaseRequestHandler {
    private final Graph graph;

    @Inject
    public EntityRelationships(final Graph graph) {
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);

        String[] ids = request.getParameterValues("ids[]");
        if (ids == null) {
            ids = new String[0];
        }

        List<String> allIds = new ArrayList<String>();

        for (int i = 0; i < ids.length; i++) {
            allIds.add(ids[i]);
        }

        JSONArray resultsJson = new JSONArray();

        Iterable<Edge> edges = graph.getRelationships(allIds, user);

        for (Edge edge : edges) {
            JSONObject rel = new JSONObject();
            rel.put("from", edge.getVertexId(Direction.OUT));
            rel.put("to", edge.getVertexId(Direction.IN));
            rel.put("relationshipType", edge.getLabel());
            resultsJson.put(rel);
        }

        respondWithJson(response, resultsJson);
    }
}
