package io.lumify.web.devTools;

import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.web.BaseRequestHandler;
import org.securegraph.Authorizations;
import org.securegraph.Graph;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ReIndexAll extends BaseRequestHandler {
    private final Graph graph;

    @Inject
    public ReIndexAll(
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            Configuration configuration,
            Graph graph,
            WorkQueueRepository workQueueRepository) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final Authorizations authorizations = getUserRepository().getAuthorizations(getUserRepository().getSystemUser());

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                graph.reindex(authorizations);
            }
        });
        t.setName("lumify-reindex");

        t.start();

        respondWithHtml(response, "Started Re-indexing thread");
    }
}
