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
import io.lumify.web.clientapi.model.ClientApiWorkspaceUpdateData;
import io.lumify.web.clientapi.model.GraphPosition;
import io.lumify.web.clientapi.model.WorkspaceAccess;
import io.lumify.web.clientapi.model.util.ObjectMapperFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public class WorkspaceUpdate extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(WorkspaceUpdate.class);
    private final WorkspaceRepository workspaceRepository;
    private final WorkQueueRepository workQueueRepository;

    @Inject
    public WorkspaceUpdate(
            final WorkspaceRepository workspaceRepository,
            final UserRepository userRepository,
            final WorkQueueRepository workQueueRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
        this.workspaceRepository = workspaceRepository;
        this.workQueueRepository = workQueueRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String workspaceId = getActiveWorkspaceId(request);
        final String data = getRequiredParameter(request, "data");

        User authUser = getUser(request);

        Workspace workspace = workspaceRepository.findById(workspaceId, authUser);
        if (workspace == null) {
            respondWithNotFound(response);
            return;
        }

        ClientApiWorkspaceUpdateData updateData = ObjectMapperFactory.getInstance().readValue(data, ClientApiWorkspaceUpdateData.class);

        if (updateData.getTitle() != null) {
            setTitle(workspace, updateData.getTitle(), authUser);
        }

        updateEntities(workspace, updateData.getEntityUpdates(), authUser);

        deleteEntities(workspace, updateData.getEntityDeletes(), authUser);

        updateUsers(workspace, updateData.getUserUpdates(), authUser);

        workspace = workspaceRepository.findById(workspaceId, authUser);
        ClientApiWorkspace clientApiWorkspaceAfterUpdateButBeforeDelete = workspaceRepository.toClientApi(workspace, authUser, true);
        workQueueRepository.pushWorkspaceChange(clientApiWorkspaceAfterUpdateButBeforeDelete);

        deleteUsers(workspace, updateData.getUserDeletes(), authUser);

        respondWithSuccessJson(response);
    }

    private void setTitle(Workspace workspace, String title, User authUser) {
        LOGGER.debug("setting title (%s): %s", workspace.getWorkspaceId(), title);
        workspaceRepository.setTitle(workspace, title, authUser);
    }

    private void deleteUsers(Workspace workspace, List<String> userDeletes, User authUser) {
        for (String userId : userDeletes) {
            LOGGER.debug("user delete (%s): %s", workspace.getWorkspaceId(), userId);
            workspaceRepository.deleteUserFromWorkspace(workspace, userId, authUser);
            workQueueRepository.pushWorkspaceDelete(workspace.getWorkspaceId(), userId);
        }
    }

    private void updateUsers(Workspace workspace, List<ClientApiWorkspaceUpdateData.UserUpdate> userUpdates, User authUser) {
        for (ClientApiWorkspaceUpdateData.UserUpdate update : userUpdates) {
            LOGGER.debug("user update (%s): %s", workspace.getWorkspaceId(), update.toString());
            String userId = update.getUserId();
            WorkspaceAccess workspaceAccess = update.getAccess();
            workspaceRepository.updateUserOnWorkspace(workspace, userId, workspaceAccess, authUser);
        }
    }

    private void deleteEntities(Workspace workspace, List<String> entityDeletes, User authUser) {
        for (String entityId : entityDeletes) {
            LOGGER.debug("workspace delete (%s): %s", workspace.getWorkspaceId(), entityId);
            workspaceRepository.softDeleteEntityFromWorkspace(workspace, entityId, authUser);
        }
    }

    private void updateEntities(Workspace workspace, List<ClientApiWorkspaceUpdateData.EntityUpdate> entityUpdates, User authUser) {
        for (ClientApiWorkspaceUpdateData.EntityUpdate update : entityUpdates) {
            LOGGER.debug("workspace update (%s): %s", workspace.getWorkspaceId(), update.toString());
            String entityId = update.getVertexId();
            GraphPosition graphPosition = update.getGraphPosition();
            workspaceRepository.updateEntityOnWorkspace(workspace, entityId, true, graphPosition, authUser);
        }
    }
}
