package io.lumify.web.routes.edge;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyResourceNotFoundException;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.GraphUtil;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import io.lumify.web.clientapi.model.ClientApiObject;
import org.securegraph.Authorizations;
import org.securegraph.Edge;
import org.securegraph.Graph;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class EdgeSourceInfo extends BaseRequestHandler {
    private final Graph graph;

    @Inject
    public EdgeSourceInfo(
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            Configuration configuration,
            Graph graph
    ) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String edgeId = getRequiredParameter(request, "edgeId");
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);

        Edge edge = this.graph.getEdge(edgeId, authorizations);
        if (edge == null) {
            throw new LumifyResourceNotFoundException("Could not find edge with id: " + edgeId, edgeId);
        }

        ClientApiObject sourceInfo = GraphUtil.getSourceInfoForEdge(graph, edge, authorizations);
        if (sourceInfo == null) {
            throw new LumifyResourceNotFoundException("Could not find source info for edge with id: " + edgeId, edgeId);
        }

        respondWithClientApiObject(response, sourceInfo);
    }
}
