package io.lumify.web.routes.vertex;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.GraphUtil;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import io.lumify.web.clientapi.model.SandboxStatus;
import io.lumify.web.routes.workspace.WorkspaceHelper;
import org.securegraph.Authorizations;
import org.securegraph.Graph;
import org.securegraph.Property;
import org.securegraph.Vertex;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

import static org.securegraph.util.IterableUtils.toList;

public class VertexDeleteProperty extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(VertexDeleteProperty.class);
    private final Graph graph;
    private final WorkspaceHelper workspaceHelper;

    @Inject
    public VertexDeleteProperty(
            final Graph graph,
            final WorkspaceHelper workspaceHelper,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
        this.workspaceHelper = workspaceHelper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String graphVertexId = getAttributeString(request, "graphVertexId");
        final String propertyName = getRequiredParameter(request, "propertyName");
        final String propertyKey = getRequiredParameter(request, "propertyKey");

        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getActiveWorkspaceId(request);

        Vertex graphVertex = graph.getVertex(graphVertexId, authorizations);
        List<Property> properties = toList(graphVertex.getProperties(propertyKey, propertyName));

        if (properties.size() == 0) {
            LOGGER.warn("Could not find property %s:%s on %s", propertyName, propertyKey, graphVertexId);
            respondWithNotFound(response);
            return;
        }

        SandboxStatus[] sandboxStatuses = GraphUtil.getPropertySandboxStatuses(properties, workspaceId);

        Property property = null;
        boolean propertyIsPublic = false;
        for (int i = 0; i < sandboxStatuses.length; i++) {
            if (property != null) {
                throw new LumifyException("Found multiple non public properties.");
            }
            property = properties.get(i);
            if (sandboxStatuses[i] == SandboxStatus.PUBLIC) {
                propertyIsPublic = true;
            }
        }

        if (property == null) {
            LOGGER.warn("Could not find property %s:%s on %s", propertyName, propertyKey, graphVertexId);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            chain.next(request, response);
            return;
        }

        workspaceHelper.deleteProperty(graphVertex, property, propertyIsPublic, workspaceId, user, authorizations);
        respondWithSuccessJson(response);
    }
}
