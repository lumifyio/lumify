package com.altamiracorp.lumify.web.routes.vertex;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.GraphUtil;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Authorizations;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.util.ConvertingIterable;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.altamiracorp.lumify.core.util.CollectionUtil.toIterable;

public class VertexMultiple extends BaseRequestHandler {
    private final Graph graph;

    @Inject
    public VertexMultiple(
            final Graph graph,
            final UserRepository userRepository,
            final Configuration configuration) {
        super(userRepository, configuration);
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String[] vertexStringIds = request.getParameterValues("vertexIds[]");
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getWorkspaceId(request);

        Iterable<Object> vertexIds = new ConvertingIterable<String, Object>(toIterable(vertexStringIds)) {
            @Override
            protected Object convert(String s) {
                return s;
            }
        };

        Iterable<Vertex> graphVertices = graph.getVertices(vertexIds, authorizations);
        JSONObject results = new JSONObject();
        JSONArray vertices = new JSONArray();
        results.put("vertices", vertices);
        for (Vertex v : graphVertices) {
            vertices.put(GraphUtil.toJson(v, workspaceId));
        }

        respondWithJson(response, results);
    }
}
