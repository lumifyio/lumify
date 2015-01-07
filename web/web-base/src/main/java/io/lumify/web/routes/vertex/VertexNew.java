package io.lumify.web.routes.vertex;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.model.workspace.Workspace;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.core.security.VisibilityTranslator;
import io.lumify.core.user.User;
import io.lumify.core.util.ClientApiConverter;
import io.lumify.core.util.GraphUtil;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import io.lumify.web.clientapi.model.ClientApiElement;
import io.lumify.web.clientapi.model.VisibilityJson;
import org.securegraph.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class VertexNew extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(VertexNew.class);

    private final Graph graph;
    private final VisibilityTranslator visibilityTranslator;
    private final WorkQueueRepository workQueueRepository;

    @Inject
    public VertexNew(
            final Graph graph,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final VisibilityTranslator visibilityTranslator,
            final WorkQueueRepository workQueueRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
        this.visibilityTranslator = visibilityTranslator;
        this.workQueueRepository = workQueueRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String conceptType = getRequiredParameter(request, "conceptType");
        final String visibilitySource = getRequiredParameter(request, "visibilitySource");
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getActiveWorkspaceId(request);

        if (!graph.isVisibilityValid(new Visibility(visibilitySource), authorizations)) {
            LOGGER.warn("%s is not a valid visibility for %s user", visibilitySource, user.getDisplayName());
            respondWithBadRequest(response, "visibilitySource", getString(request, "visibility.invalid"), visibilitySource);
            chain.next(request, response);
            return;
        }

        respondWithClientApiObject(response, handle(conceptType, visibilitySource, user, workspaceId, authorizations));
    }

    private ClientApiElement handle(String conceptType, String visibilitySource, User user, String workspaceId, Authorizations authorizations) {
        Workspace workspace = getWorkspaceRepository().findById(workspaceId, user);

        VisibilityJson visibilityJson = GraphUtil.updateVisibilitySourceAndAddWorkspaceId(null, visibilitySource, workspaceId);
        LumifyVisibility lumifyVisibility = this.visibilityTranslator.toVisibility(visibilityJson);
        Visibility visibility = lumifyVisibility.getVisibility();

        VertexBuilder vertexBuilder = this.graph.prepareVertex(visibility);
        LumifyProperties.VISIBILITY_JSON.setProperty(vertexBuilder, visibilityJson, visibility);
        Metadata propertyMetadata = new Metadata();
        LumifyProperties.VISIBILITY_JSON.setMetadata(propertyMetadata, visibilityJson, visibilityTranslator.getDefaultVisibility());
        LumifyProperties.CONCEPT_TYPE.setProperty(vertexBuilder, conceptType, propertyMetadata, visibility);
        Vertex vertex = vertexBuilder.save(authorizations);
        this.graph.flush();

        getWorkspaceRepository().updateEntityOnWorkspace(workspace, vertex.getId(), true, null, user);
        this.graph.flush();

        LOGGER.debug("Created new empty vertex with id: %s", vertex.getId());

        workQueueRepository.pushElement(vertex);
        workQueueRepository.pushGraphPropertyQueue(vertex, null, LumifyProperties.CONCEPT_TYPE.getPropertyName(), workspaceId, visibilitySource);
        workQueueRepository.pushUserCurrentWorkspaceChange(user, workspaceId);

        return ClientApiConverter.toClientApi(vertex, workspaceId, authorizations);
    }
}
