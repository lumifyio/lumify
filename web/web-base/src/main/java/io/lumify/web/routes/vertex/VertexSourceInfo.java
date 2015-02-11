package io.lumify.web.routes.vertex;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyResourceNotFoundException;
import io.lumify.core.model.termMention.TermMentionRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import io.lumify.web.clientapi.model.ClientApiObject;
import org.securegraph.Authorizations;
import org.securegraph.Graph;
import org.securegraph.Vertex;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class VertexSourceInfo extends BaseRequestHandler {
    private final Graph graph;
    private final TermMentionRepository termMentionRepository;

    @Inject
    public VertexSourceInfo(
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            Configuration configuration,
            Graph graph,
            TermMentionRepository termMentionRepository
    ) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
        this.termMentionRepository = termMentionRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String vertexId = getRequiredParameter(request, "vertexId");
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);

        Vertex vertex = this.graph.getVertex(vertexId, authorizations);
        if (vertex == null) {
            throw new LumifyResourceNotFoundException("Could not find vertex with id: " + vertexId, vertexId);
        }

        ClientApiObject sourceInfo = termMentionRepository.getSourceInfoForVertex(vertex, authorizations);
        if (sourceInfo == null) {
            throw new LumifyResourceNotFoundException("Could not find source info for vertex with id: " + vertexId, vertexId);
        }

        respondWithClientApiObject(response, sourceInfo);
    }
}
