package io.lumify.web.routes.graph;

import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.JsonSerializer;
import io.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import org.securegraph.Authorizations;
import org.securegraph.Graph;
import org.securegraph.Path;
import org.securegraph.Vertex;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GraphFindPath extends BaseRequestHandler {
    private final Graph graph;

    @Inject
    public GraphFindPath(
            final Graph graph,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getActiveWorkspaceId(request);

        final String sourceGraphVertexId = getRequiredParameter(request, "sourceGraphVertexId");
        final String destGraphVertexId = getRequiredParameter(request, "destGraphVertexId");
        final int hops = Integer.parseInt(getRequiredParameter(request, "hops"));

        Vertex sourceVertex = graph.getVertex(sourceGraphVertexId, authorizations);
        if (sourceVertex == null) {
            respondWithNotFound(response, "Source vertex not found");
            return;
        }

        Vertex destVertex = graph.getVertex(destGraphVertexId, authorizations);
        if (destVertex == null) {
            respondWithNotFound(response, "Destination vertex not found");
            return;
        }

        JSONObject resultsJson = new JSONObject();
        JSONArray pathResults = new JSONArray();

        Iterable<Path> paths = graph.findPaths(sourceVertex, destVertex, hops, authorizations);
        for (Path path : paths) {
            JSONArray verticesJson = JsonSerializer.toJson(graph.getVerticesInOrder(path, authorizations), workspaceId);
            pathResults.put(verticesJson);
        }

        resultsJson.put("paths", pathResults);

        respondWithJson(response, resultsJson);
    }
}

