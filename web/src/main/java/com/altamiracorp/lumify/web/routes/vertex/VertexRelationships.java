package com.altamiracorp.lumify.web.routes.vertex;

import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.model.graph.GraphRelationship;
import com.altamiracorp.lumify.core.model.graph.GraphRepository;
import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class VertexRelationships extends BaseRequestHandler {
    private final GraphRepository graphRepository;

    @Inject
    public VertexRelationships(final GraphRepository repo) {
        graphRepository = repo;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        String graphVertexId = (String) request.getAttribute("graphVertexId");
        long offset = getOptionalParameterLong(request, "offset", 0);
        long size = getOptionalParameterLong(request, "size", 25);

        Map<GraphRelationship, GraphVertex> relationships = graphRepository.getRelationships(graphVertexId, user);

        JSONObject json = new JSONObject();
        JSONArray relationshipsJson = new JSONArray();
        long referencesAdded = 0, skipped = 0, totalReferences = 0;
        for (Map.Entry<GraphRelationship, GraphVertex> relationship : relationships.entrySet()) {
            if (relationship.getKey().getLabel().equals("hasEntity")) {
                totalReferences++;
                if (referencesAdded >= size) continue;
                if (skipped < offset) {
                    skipped++;
                    continue;
                }

                referencesAdded++;
            }

            JSONObject relationshipJson = new JSONObject();
            relationshipJson.put("relationship", relationship.getKey().toJson());
            relationshipJson.put("vertex", relationship.getValue().toJson());
            relationshipsJson.put(relationshipJson);
        }
        json.put("totalReferences", totalReferences);
        json.put("relationships", relationshipsJson);

        respondWithJson(response, json);
    }
}
