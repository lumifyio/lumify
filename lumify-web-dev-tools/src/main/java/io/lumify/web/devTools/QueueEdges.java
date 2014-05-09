package io.lumify.web.devTools;

import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.web.BaseRequestHandler;
import org.securegraph.Authorizations;
import org.securegraph.Edge;
import org.securegraph.Graph;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class QueueEdges extends BaseRequestHandler {
    private final Graph graph;
    private final WorkQueueRepository workQueueRepository;

    @Inject
    public QueueEdges(
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            Configuration configuration,
            Graph graph,
            WorkQueueRepository workQueueRepository) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
        this.workQueueRepository = workQueueRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);

        Iterable<Edge> edges = graph.getEdges(authorizations);
        for (Edge edge : edges) {
            workQueueRepository.pushElement(edge);
        }
        workQueueRepository.flush();

        respondWithHtml(response, "OK");
    }
}
