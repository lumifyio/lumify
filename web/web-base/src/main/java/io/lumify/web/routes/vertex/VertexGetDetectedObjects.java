package io.lumify.web.routes.vertex;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.ClientApiConverter;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import io.lumify.web.clientapi.model.ClientApiDetectedObjects;
import org.securegraph.Authorizations;
import org.securegraph.Graph;
import org.securegraph.Property;
import org.securegraph.Vertex;
import org.securegraph.util.IterableUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


// This route will no longer be needed once we refactor detected objects.
public class VertexGetDetectedObjects extends BaseRequestHandler {
    private final Graph graph;

    @Inject
    public VertexGetDetectedObjects(
            Graph graph,
            UserRepository userRepository,
            Configuration configuration,
            final WorkspaceRepository workspaceRepository) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String graphVertexId = getRequiredParameter(request, "graphVertexId");
        String propertyName = getRequiredParameter(request, "propertyName");
        String workspaceId = getRequiredParameter(request, "workspaceId");

        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);

        Vertex vertex = graph.getVertex(graphVertexId, authorizations);
        if (vertex == null) {
            respondWithNotFound(response, String.format("vertex %s not found", graphVertexId));
            return;
        }

        ClientApiDetectedObjects detectedObjects = new ClientApiDetectedObjects();
        Iterable<Property> detectedObjectProperties = vertex.getProperties(propertyName);
        if (detectedObjectProperties == null || IterableUtils.count(detectedObjectProperties) == 0) {
            respondWithNotFound(response, String.format("property %s not found on vertex %s", propertyName, vertex.getId()));
            return;
        }
        detectedObjects.addDetectedObjects(ClientApiConverter.toClientApiProperties(detectedObjectProperties, workspaceId));

        respondWithClientApiObject(response, detectedObjects);
    }

}
