package io.lumify.web.routes.vertex;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.longRunningProcess.FindPathLongRunningProcessQueueItem;
import io.lumify.core.model.longRunningProcess.LongRunningProcessRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import io.lumify.web.clientapi.model.ClientApiLongRunningProcessSubmitResponse;
import org.securegraph.Authorizations;
import org.securegraph.Graph;
import org.securegraph.Vertex;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class VertexFindPath extends BaseRequestHandler {
    private final Graph graph;
    private final LongRunningProcessRepository longRunningProcessRepository;

    @Inject
    public VertexFindPath(
            final Graph graph,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration,
            final LongRunningProcessRepository longRunningProcessRepository) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
        this.longRunningProcessRepository = longRunningProcessRepository;
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

        FindPathLongRunningProcessQueueItem findPathQueueItem = new FindPathLongRunningProcessQueueItem(sourceVertex.getId(), destVertex.getId(), hops, workspaceId, authorizations);
        String id = this.longRunningProcessRepository.enqueue(findPathQueueItem.toJson(), user, authorizations);

        respondWithClientApiObject(response, new ClientApiLongRunningProcessSubmitResponse(id));
    }
}

