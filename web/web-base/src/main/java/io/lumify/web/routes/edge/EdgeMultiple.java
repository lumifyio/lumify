package io.lumify.web.routes.edge;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyAccessDeniedException;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.ClientApiConverter;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import io.lumify.web.clientapi.model.ClientApiEdgeMultipleResponse;
import io.lumify.web.clientapi.model.ClientApiEdgeWithVertexData;
import io.lumify.web.clientapi.model.ClientApiVertexMultipleResponse;
import org.securegraph.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashSet;

import static org.securegraph.util.IterableUtils.toIterable;

public class EdgeMultiple extends BaseRequestHandler {
    private final Graph graph;

    @Inject
    public EdgeMultiple(
            final Graph graph,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        HashSet<String> edgeStringIds = new HashSet<String>(Arrays.asList(getRequiredParameterArray(request, "edgeIds[]")));

        User user = getUser(request);
        GetAuthorizationsResult getAuthorizationsResult = getAuthorizations(request, false, user);
        String workspaceId = getWorkspaceId(request);

        Iterable<String> edgeIds = toIterable(edgeStringIds.toArray(new String[edgeStringIds.size()]));
        Iterable<Edge> graphEdges = graph.getEdges(edgeIds, FetchHint.ALL, getAuthorizationsResult.authorizations);
        ClientApiEdgeMultipleResponse result = new ClientApiEdgeMultipleResponse();
        for (Edge e : graphEdges) {
            Vertex source = e.getVertex(Direction.OUT, getAuthorizationsResult.authorizations);
            Vertex dest = e.getVertex(Direction.IN, getAuthorizationsResult.authorizations);
            result.getEdges().add((ClientApiEdgeWithVertexData) ClientApiConverter.toClientApiEdgeWithVertexData(e, source, dest, workspaceId, getAuthorizationsResult.authorizations));
        }

        respondWithClientApiObject(response, result);
    }

    private GetAuthorizationsResult getAuthorizations(HttpServletRequest request, boolean fallbackToPublic, User user) {
        GetAuthorizationsResult result = new GetAuthorizationsResult();
        result.requiredFallback = false;
        try {
            result.authorizations = getAuthorizations(request, user);
        } catch (LumifyAccessDeniedException ex) {
            if (fallbackToPublic) {
                result.authorizations = getUserRepository().getAuthorizations(user);
                result.requiredFallback = true;
            } else {
                throw ex;
            }
        }
        return result;
    }

    private String getWorkspaceId(HttpServletRequest request) {
        String workspaceId;
        try {
            workspaceId = getActiveWorkspaceId(request);
        } catch (LumifyException ex) {
            workspaceId = null;
        }
        return workspaceId;
    }

    private static class GetAuthorizationsResult {
        public Authorizations authorizations;
        public boolean requiredFallback;
    }
}
