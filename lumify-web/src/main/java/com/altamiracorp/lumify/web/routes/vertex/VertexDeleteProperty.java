package com.altamiracorp.lumify.web.routes.vertex;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.exception.LumifyException;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.model.workspace.diff.SandboxStatus;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.GraphUtil;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.lumify.web.routes.workspace.WorkspaceHelper;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Authorizations;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Property;
import com.altamiracorp.securegraph.Vertex;
import com.google.inject.Inject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

import static com.altamiracorp.securegraph.util.IterableUtils.toList;

public class VertexDeleteProperty extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(VertexDeleteProperty.class);
    private final Graph graph;
    private final WorkspaceHelper workspaceHelper;

    @Inject
    public VertexDeleteProperty(
            final Graph graph,
            final WorkspaceHelper workspaceHelper,
            final UserRepository userRepository,
            final Configuration configuration) {
        super(userRepository, configuration);
        this.graph = graph;
        this.workspaceHelper = workspaceHelper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String graphVertexId = getAttributeString(request, "graphVertexId");
        final String propertyName = getRequiredParameter(request, "propertyName");

        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getWorkspaceId(request);

        Vertex graphVertex = graph.getVertex(graphVertexId, authorizations);
        List<Property> properties = toList(graphVertex.getProperties(propertyName));

        if (properties.size() == 0) {
            LOGGER.warn("Could not find property: %s", propertyName);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            chain.next(request, response);
            return;
        }

        SandboxStatus[] sandboxStatuses = GraphUtil.getPropertySandboxStatuses(properties, workspaceId);

        Property property = null;
        for (int i = 0; i < sandboxStatuses.length; i++) {
            if (sandboxStatuses[i] == SandboxStatus.PUBLIC) {
                continue;
            }
            if (property != null) {
                throw new LumifyException("Found multiple non public properties.");
            }
            property = properties.get(i);
        }

        if (property == null) {
            LOGGER.warn("Could not find non-public property: %s", propertyName);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            chain.next(request, response);
            return;
        }

        respondWithJson(response, workspaceHelper.deleteProperty(graphVertex, properties, property, workspaceId));
    }
}
