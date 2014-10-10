package io.lumify.web.routes.vertex;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.termMention.TermMentionRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.ClientApiConverter;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import io.lumify.web.clientapi.model.ClientApiTermMentionsResponse;
import org.securegraph.Authorizations;
import org.securegraph.Graph;
import org.securegraph.Property;
import org.securegraph.Vertex;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class VertexGetTermMentions extends BaseRequestHandler {
    private final Graph graph;
    private final TermMentionRepository termMentionRepository;

    @Inject
    public VertexGetTermMentions(
            Graph graph,
            UserRepository userRepository,
            Configuration configuration,
            final WorkspaceRepository workspaceRepository,
            TermMentionRepository termMentionRepository) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
        this.termMentionRepository = termMentionRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String graphVertexId = getRequiredParameter(request, "graphVertexId");
        String propertyName = getRequiredParameter(request, "propertyName");
        String propertyKey = getRequiredParameter(request, "propertyKey");

        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getActiveWorkspaceId(request);

        Vertex vertex = graph.getVertex(graphVertexId, authorizations);
        if (vertex == null) {
            respondWithNotFound(response, String.format("vertex %s not found", graphVertexId));
            return;
        }

        Property property = vertex.getProperty(propertyKey, propertyName);
        if (property == null) {
            respondWithNotFound(response, String.format("property %s:%s not found on vertex %s", propertyKey, propertyName, vertex.getId()));
            return;
        }

        Iterable<Vertex> termMentions = termMentionRepository.findBySourceGraphVertexAndPropertyKey(graphVertexId, propertyKey, authorizations);
        ClientApiTermMentionsResponse termMentionsResponse = ClientApiConverter.toTermMentionsResponse(termMentions, workspaceId, authorizations);
        respondWithClientApiObject(response, termMentionsResponse);
    }
}
