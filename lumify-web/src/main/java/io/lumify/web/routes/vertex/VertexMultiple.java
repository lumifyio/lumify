package io.lumify.web.routes.vertex;

import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyAccessDeniedException;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.JsonSerializer;
import io.lumify.web.BaseRequestHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import org.securegraph.Authorizations;
import org.securegraph.Graph;
import org.securegraph.Vertex;
import org.securegraph.util.ConvertingIterable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.securegraph.util.IterableUtils.toIterable;

public class VertexMultiple extends BaseRequestHandler {
    private final Graph graph;

    @Inject
    public VertexMultiple(
            final Graph graph,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String[] vertexStringIds = getRequiredParameterArray(request, "vertexIds[]");
        boolean fallbackToPublic = getOptionalParameterBoolean(request, "fallbackToPublic", false);
        User user = getUser(request);
        GetAuthorizationsResult getAuthorizationsResult = getAuthorizations(request, fallbackToPublic, user);
        String workspaceId = getWorkspaceId(request);

        Iterable<Object> vertexIds = new ConvertingIterable<String, Object>(toIterable(vertexStringIds)) {
            @Override
            protected Object convert(String s) {
                return s;
            }
        };

        Iterable<Vertex> graphVertices = graph.getVertices(vertexIds, getAuthorizationsResult.authorizations);
        JSONObject results = new JSONObject();
        JSONArray vertices = new JSONArray();
        results.put("vertices", vertices);
        results.put("requiredFallback", getAuthorizationsResult.requiredFallback);
        for (Vertex v : graphVertices) {
            vertices.put(JsonSerializer.toJson(v, workspaceId));
        }

        respondWithJson(response, results);
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
