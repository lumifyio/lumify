package io.lumify.web.routes.edge;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyResourceNotFoundException;
import io.lumify.core.model.termMention.TermMentionRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import io.lumify.web.clientapi.model.ClientApiObject;
import org.securegraph.Authorizations;
import org.securegraph.Edge;
import org.securegraph.Graph;
import org.securegraph.Visibility;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class EdgePropertySourceInfo extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(EdgePropertySourceInfo.class);
    private final Graph graph;
    private final TermMentionRepository termMentionRepository;

    @Inject
    public EdgePropertySourceInfo(
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
        String edgeId = getRequiredParameter(request, "edgeId");
        String propertyName = getRequiredParameter(request, "propertyName");
        String visibilitySource = getRequiredParameter(request, "visibilitySource");
        String propertyKey = getOptionalParameter(request, "propertyKey");
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);

        Visibility visibility = new Visibility(visibilitySource);
        if (!graph.isVisibilityValid(visibility, authorizations)) {
            LOGGER.warn("%s is not a valid visibility for %s user", visibilitySource, user.getDisplayName());
            respondWithBadRequest(response, "visibilitySource", getString(request, "visibility.invalid"));
            chain.next(request, response);
            return;
        }

        Edge edge = this.graph.getEdge(edgeId, authorizations);
        if (edge == null) {
            throw new LumifyResourceNotFoundException("Could not find edge with id: " + edgeId, edgeId);
        }

        ClientApiObject sourceInfo = termMentionRepository.getSourceInfoForEdgeProperty(edge, propertyKey, propertyName, visibility, authorizations);
        if (sourceInfo == null) {
            throw new LumifyResourceNotFoundException("Could not find source info for edge with id: " + edgeId, edgeId);
        }

        respondWithClientApiObject(response, sourceInfo);
    }
}
