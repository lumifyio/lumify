package com.altamiracorp.lumify.web.routes.entity;

import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Authorizations;
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

import static com.altamiracorp.lumify.core.util.CollectionUtil.toList;

public class EntityRelationships extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(EntityRelationships.class);
    private final Graph graph;
    private final UserRepository userRepository;

    @Inject
    public EntityRelationships(final Graph graph, final UserRepository userRepository) {
        this.graph = graph;
        this.userRepository = userRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        Authorizations authorizations = userRepository.getAuthorizations(user);

        long startTime = System.nanoTime();

        String[] ids = request.getParameterValues("ids[]");
        if (ids == null) {
            ids = new String[0];
        }

        List<Object> allIds = new ArrayList<Object>();
        for (int i = 0; i < ids.length; i++) {
            allIds.add(ids[i]);
        }

        JSONArray resultsJson = new JSONArray();

        List<Edge> edges = toList(graph.getEdges(graph.findRelatedEdges(allIds, authorizations), authorizations));
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
}
