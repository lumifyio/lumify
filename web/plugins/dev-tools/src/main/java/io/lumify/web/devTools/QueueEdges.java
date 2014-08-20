package io.lumify.web.devTools;

import io.lumify.miniweb.HandlerChain;
import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.web.BaseRequestHandler;
import org.securegraph.Authorizations;
import org.securegraph.Edge;
import org.securegraph.Graph;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class QueueEdges extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(QueueEdges.class);
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
        final Authorizations authorizations = getUserRepository().getAuthorizations(getUserRepository().getSystemUser());

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                LOGGER.info("requeue all edges");
                Iterable<Edge> edges = graph.getEdges(authorizations);
                for (Edge edge : edges) {
                    workQueueRepository.pushElement(edge);
                }
                workQueueRepository.flush();
                LOGGER.info("requeue all edges complete");
            }
        });
        t.setName("requeue-edges");
        t.start();

        respondWithHtml(response, "Started requeue thread");
    }
}
