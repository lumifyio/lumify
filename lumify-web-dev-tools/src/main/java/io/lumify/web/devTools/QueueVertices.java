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
import org.securegraph.Graph;
import org.securegraph.Vertex;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class QueueVertices extends BaseRequestHandler {
    private final Graph graph;
    private final WorkQueueRepository workQueueRepository;

    @Inject
    public QueueVertices(
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

        Iterable<Vertex> vertices = graph.getVertices(authorizations);
        for (Vertex vertex : vertices) {
            workQueueRepository.pushElement(vertex);
        }
        workQueueRepository.flush();

        respondWithHtml(response, "OK");
    }
}
