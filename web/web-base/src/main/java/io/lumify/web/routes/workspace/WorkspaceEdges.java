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
import io.lumify.web.clientapi.model.ClientApiEdge;
import io.lumify.web.clientapi.model.ClientApiWorkspaceEdges;
import org.securegraph.Authorizations;
import org.securegraph.Edge;
import org.securegraph.Graph;
import org.securegraph.util.ConvertingIterable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.List;

import static org.securegraph.util.IterableUtils.toList;

public class WorkspaceEdges extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(WorkspaceEdges.class);
    private final Graph graph;
    private final WorkspaceRepository workspaceRepository;

    @Inject
    public WorkspaceEdges(
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
        String[] additionalIds = getOptionalParameterAsStringArray(request, "ids[]"); // additional graph vertex ids to search for
        if (additionalIds == null) {
            additionalIds = new String[0];
        }

        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getActiveWorkspaceId(request);

        long startTime = System.nanoTime();

        Workspace workspace = workspaceRepository.findById(workspaceId, user);
        List<WorkspaceEntity> workspaceEntities = workspaceRepository.findEntities(workspace, user);
        List<String> allIds = toList(new ConvertingIterable<WorkspaceEntity, String>(workspaceEntities) {
            @Override
            protected String convert(WorkspaceEntity workspaceEntity) {
                return workspaceEntity.getEntityVertexId();
            }
        });
        Collections.addAll(allIds, additionalIds);

        List<Edge> edges = toList(graph.getEdges(graph.findRelatedEdges(allIds, authorizations), authorizations));
        ClientApiWorkspaceEdges results = new ClientApiWorkspaceEdges();
        for (Edge edge : edges) {
            ClientApiEdge e = new ClientApiEdge();
            ClientApiConverter.populateClientApiEdge(e, edge, workspaceId);
            results.getEdges().add(e);
        }

        long endTime = System.nanoTime();
        LOGGER.debug("Retrieved %d in %dms", edges.size(), (endTime - startTime) / 1000 / 1000);

        respondWithClientApiObject(response, results);
    }
}
