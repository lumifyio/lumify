package io.lumify.web.routes.workspace;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.Workspace;
import io.lumify.core.model.workspace.WorkspaceEntity;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.ClientApiConverter;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import io.lumify.web.clientapi.model.ClientApiWorkspaceVertices;
import org.securegraph.Authorizations;
import org.securegraph.Graph;
import org.securegraph.Vertex;
import org.securegraph.util.LookAheadIterable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Iterator;
import java.util.List;

public class WorkspaceVertices extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(WorkspaceVertices.class);
    private final Graph graph;
    private final WorkspaceRepository workspaceRepository;

    @Inject
    public WorkspaceVertices(
            final Graph graph,
            final UserRepository userRepository,
            final Configuration configuration,
            final WorkspaceRepository workspaceRepository) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
        this.workspaceRepository = workspaceRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getActiveWorkspaceId(request);

        Workspace workspace = workspaceRepository.findById(workspaceId, user);
        final List<WorkspaceEntity> workspaceEntities = workspaceRepository.findEntities(workspace, user);
        Iterable<String> vertexIds = getVisibleWorkspaceEntityIds(workspaceEntities);
        Iterable<Vertex> graphVertices = graph.getVertices(vertexIds, authorizations);
        ClientApiWorkspaceVertices results = new ClientApiWorkspaceVertices();
        results.getVertices().addAll(ClientApiConverter.toClientApiVertices(graphVertices, workspaceId, authorizations));
        respondWithClientApiObject(response, results);
    }

    private LookAheadIterable<WorkspaceEntity, String> getVisibleWorkspaceEntityIds(final List<WorkspaceEntity> workspaceEntities) {
        return new LookAheadIterable<WorkspaceEntity, String>() {
            @Override
            protected boolean isIncluded(WorkspaceEntity workspaceEntity, String entityVertexId) {
                return workspaceEntity.isVisible();
            }

            @Override
            protected String convert(WorkspaceEntity workspaceEntity) {
                return workspaceEntity.getEntityVertexId();
            }

            @Override
            protected Iterator<WorkspaceEntity> createIterator() {
                return workspaceEntities.iterator();
            }
        };
    }
}
