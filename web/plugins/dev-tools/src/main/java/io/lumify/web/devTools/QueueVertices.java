package io.lumify.web.devTools;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import org.securegraph.Authorizations;
import org.securegraph.Graph;
import org.securegraph.Property;
import org.securegraph.Vertex;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class QueueVertices extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(QueueVertices.class);
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
        String propertyName = getOptionalParameter(request, "propertyName");
        if (propertyName != null && propertyName.trim().length() == 0) {
            propertyName = null;
        }
        final Authorizations authorizations = getUserRepository().getAuthorizations(getUserRepository().getSystemUser());

        final String finalPropertyName = propertyName;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                LOGGER.info("requeue all vertices (property: %s)", finalPropertyName);
                int count = 0;
                int pushedCount = 0;
                Iterable<Vertex> vertices = graph.getVertices(authorizations);
                for (Vertex vertex : vertices) {
                    if (finalPropertyName == null) {
                        workQueueRepository.pushElement(vertex);
                        pushedCount++;
                    } else {
                        Iterable<Property> properties = vertex.getProperties(finalPropertyName);
                        for (Property property : properties) {
                            workQueueRepository.pushGraphPropertyQueue(vertex, property);
                            pushedCount++;
                        }
                    }
                    count++;
                    if ((count % 10000) == 0) {
                        LOGGER.debug("requeue status. vertices looked at %d. items pushed %d. last vertex id: %s", count, pushedCount, vertex.getId());
                    }
                }
                workQueueRepository.flush();
                LOGGER.info("requeue all vertices complete. vertices looked at %d. items pushed %d.", count, pushedCount);
            }
        });
        t.setName("requeue-vertices");
        t.start();

        respondWithHtml(response, "Started requeue thread");
    }
}
