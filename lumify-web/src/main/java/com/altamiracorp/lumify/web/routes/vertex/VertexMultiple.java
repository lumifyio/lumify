package com.altamiracorp.lumify.web.routes.vertex;

import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.GraphUtil;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.google.inject.Inject;
import org.json.JSONArray;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

public class VertexMultiple extends BaseRequestHandler {
    private final Graph graph;

    @Inject
    public VertexMultiple(final Graph graph) {
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String[] vertexIds = request.getParameterValues("vertexIds[]");
        User user = getUser(request);

        List<Vertex> graphVertices = findAllVertices(vertexIds, user);

        Iterator<Vertex> i = graphVertices.iterator();
        JSONArray results = new JSONArray();
        while (i.hasNext()) {
            Vertex vertex = i.next();
            if (vertex == null) {
                i.remove();
            } else {
                results.put(GraphUtil.toJson(vertex));
            }
        }

        respondWithJson(response,results);
    }

    private List<Vertex> findAllVertices (String[] vertexIds, User user) {
        List<Vertex> vertices = new ArrayList<Vertex>();
        for (String id : vertexIds) {
            vertices.add(graph.getVertex(id, user.getAuthorizations()));
        }
        return vertices;
    }
}
