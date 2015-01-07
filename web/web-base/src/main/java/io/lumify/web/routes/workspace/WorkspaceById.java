package io.lumify.web.routes.workspace;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.Workspace;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import io.lumify.web.clientapi.model.ClientApiWorkspace;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WorkspaceById extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(WorkspaceById.class);

    @Inject
    public WorkspaceById(
            final WorkspaceRepository workspaceRepo,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String workspaceId = super.getAttributeString(request, "workspaceId");
        final User authUser = getUser(request);
        LOGGER.info("Attempting to retrieve workspace: %s", workspaceId);
        final Workspace workspace = getWorkspaceRepository().findById(workspaceId, authUser);
        if (workspace == null) {
            LOGGER.warn("Could not find workspace: %s", workspaceId);
            respondWithNotFound(response);
        } else {
            LOGGER.debug("Successfully found workspace");
            ClientApiWorkspace result = getWorkspaceRepository().toClientApi(workspace, authUser, true);
            respondWithClientApiObject(response, result);
        }
    }
}
