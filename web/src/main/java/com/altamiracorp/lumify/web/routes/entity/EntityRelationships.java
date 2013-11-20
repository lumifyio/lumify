package com.altamiracorp.lumify.web.routes.entity;

import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.model.graph.GraphRelationship;
import com.altamiracorp.lumify.core.model.graph.GraphRepository;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

public class EntityRelationships extends BaseRequestHandler {
    private final GraphRepository graphRepository;

    @Inject
    public EntityRelationships(final GraphRepository repo) {
        graphRepository = repo;
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

        List<GraphRelationship> graphRelationships = graphRepository.getRelationships(allIds, user);

        for (GraphRelationship graphRelationship : graphRelationships) {
            JSONObject rel = new JSONObject();
            rel.put("from", graphRelationship.getSourceVertexId());
            rel.put("to", graphRelationship.getDestVertexId());
            rel.put("relationshipType", graphRelationship.getLabel());
            resultsJson.put(rel);
        }

        respondWithJson(response, resultsJson);
    }
}
