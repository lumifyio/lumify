package io.lumify.web.routes.vertex;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import io.lumify.web.clientapi.model.ClientApiVerticesExistsResponse;
import org.securegraph.Authorizations;
import org.securegraph.Graph;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class VertexExists extends BaseRequestHandler {
    private final Graph graph;

    @Inject
    public VertexExists(
            final Graph graph,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        List<String> vertexIds = Arrays.asList(getRequiredParameterArray(request, "vertexIds[]"));
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);

        Map<String, Boolean> graphVertices = graph.doVerticesExist(vertexIds, authorizations);
        ClientApiVerticesExistsResponse result = new ClientApiVerticesExistsResponse();
        result.getExists().putAll(graphVertices);

        respondWithClientApiObject(response, result);
    }
}
