package com.altamiracorp.lumify.web.routes.vertex;

import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.*;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.altamiracorp.lumify.core.util.GraphUtil.toJson;

public class VertexRelationships extends BaseRequestHandler {
    private final Graph graph;
    private final UserRepository userRepository;

    @Inject
    public VertexRelationships(final Graph graph, final UserRepository userRepository) {
        this.graph = graph;
        this.userRepository = userRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        Authorizations authorizations = userRepository.getAuthorizations(user);

        String graphVertexId = (String) request.getAttribute("graphVertexId");
        long offset = getOptionalParameterLong(request, "offset", 0);
        long size = getOptionalParameterLong(request, "size", 25);

        Vertex vertex = graph.getVertex(graphVertexId, authorizations);
        Iterable<Edge> edges = vertex.getEdges(Direction.BOTH, authorizations);

        JSONObject json = new JSONObject();
        JSONArray relationshipsJson = new JSONArray();
        long referencesAdded = 0, skipped = 0, totalReferences = 0;
        for (Edge edge : edges) {
            if (edge.getLabel().equals("hasEntity")) {
                totalReferences++;
                if (referencesAdded >= size) continue;
                if (skipped < offset) {
                    skipped++;
                    continue;
                }

                referencesAdded++;
            }

            JSONObject relationshipJson = new JSONObject();
            relationshipJson.put("relationship", toJson(edge));
            Vertex otherVertex = edge.getOtherVertex(vertex.getId(), authorizations);
            relationshipJson.put("vertex", toJson(otherVertex));
            relationshipsJson.put(relationshipJson);
        }
        json.put("totalReferences", totalReferences);
        json.put("relationships", relationshipsJson);

        respondWithJson(response, json);
    }
}
