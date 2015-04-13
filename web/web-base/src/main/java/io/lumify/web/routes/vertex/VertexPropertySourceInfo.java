package io.lumify.web.routes.vertex;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyResourceNotFoundException;
import io.lumify.core.model.SourceInfo;
import io.lumify.core.model.termMention.TermMentionRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.core.security.VisibilityTranslator;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import io.lumify.web.clientapi.model.VisibilityJson;
import org.securegraph.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class VertexPropertySourceInfo extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(VertexPropertySourceInfo.class);
    private final Graph graph;
    private final VisibilityTranslator visibilityTranslator;
    private final TermMentionRepository termMentionRepository;

    @Inject
    public VertexPropertySourceInfo(
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            Configuration configuration,
            Graph graph,
            VisibilityTranslator visibilityTranslator,
            TermMentionRepository termMentionRepository
    ) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
        this.visibilityTranslator = visibilityTranslator;
        this.termMentionRepository = termMentionRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String vertexId = getRequiredParameter(request, "vertexId");
        String propertyName = getRequiredParameter(request, "propertyName");
        String visibilitySource = getRequiredParameter(request, "visibilitySource");
        String propertyKey = getOptionalParameter(request, "propertyKey");
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getActiveWorkspaceId(request);

        Visibility visibility = new Visibility(visibilitySource);
        if (!graph.isVisibilityValid(visibility, authorizations)) {
            LOGGER.warn("%s is not a valid visibility for %s user", visibilitySource, user.getDisplayName());
            respondWithBadRequest(response, "visibilitySource", getString(request, "visibility.invalid"));
            chain.next(request, response);
            return;
        }

        Vertex vertex = this.graph.getVertex(vertexId, authorizations);
        if (vertex == null) {
            throw new LumifyResourceNotFoundException("Could not find vertex with id: " + vertexId, vertexId);
        }

        Property property = vertex.getProperty(propertyKey, propertyName, visibility);
        if (property == null) {
            VisibilityJson visibilityJson = new VisibilityJson();
            visibilityJson.setSource(visibilitySource);
            visibilityJson.addWorkspace(workspaceId);
            LumifyVisibility v2 = visibilityTranslator.toVisibility(visibilityJson);
            property = vertex.getProperty(propertyKey, propertyName, v2.getVisibility());
            if (property == null) {
                throw new LumifyResourceNotFoundException("Could not find property " + propertyKey + ":" + propertyName + ":" + visibility + " on vertex with id: " + vertexId, vertexId);
            }
        }

        SourceInfo sourceInfo = termMentionRepository.getSourceInfoForVertexProperty(vertex.getId(), property, authorizations);
        if (sourceInfo == null) {
            respondWithNotFound(response, "No source info for vertex " + vertex.getId() + " property " + property);
            return;
        }

        respondWithClientApiObject(response, sourceInfo);
    }
}
