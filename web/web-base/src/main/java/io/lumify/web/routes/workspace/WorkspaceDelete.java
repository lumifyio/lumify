package io.lumify.web.routes.workspace;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
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

public class WorkspaceDelete extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(WorkspaceDelete.class);
    private final WorkspaceRepository workspaceRepository;
    private final WorkQueueRepository workQueueRepository;

    @Inject
    public WorkspaceDelete(
            final WorkspaceRepository workspaceRepository,
            final WorkQueueRepository workQueueRepository,
            final UserRepository userRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
        this.workspaceRepository = workspaceRepository;
        this.workQueueRepository = workQueueRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        if (isDeleteAuthorized(request)) {
            final String workspaceId = getAttributeString(request, "workspaceId");

            User user = getUser(request);

            LOGGER.info("Deleting workspace with id: %s", workspaceId);
            Workspace workspace = workspaceRepository.findById(workspaceId, user);
            if (workspace == null) {
                respondWithNotFound(response);
                return;
            }
            ClientApiWorkspace clientApiWorkspaceBeforeDeletion = workspaceRepository.toClientApi(workspace, user, false);
            workspaceRepository.delete(workspace, user);
            workQueueRepository.pushWorkspaceDelete(clientApiWorkspaceBeforeDeletion);

            respondWithSuccessJson(response);
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    // TODO: Make this workspace delete authorization more robust
    private boolean isDeleteAuthorized(HttpServletRequest request) {
        return true;
    }
}
