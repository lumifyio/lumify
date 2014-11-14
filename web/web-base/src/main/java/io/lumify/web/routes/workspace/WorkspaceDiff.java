package io.lumify.web.routes.workspace;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.Workspace;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import io.lumify.web.clientapi.model.ClientApiWorkspaceDiff;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WorkspaceDiff extends BaseRequestHandler {
    private final WorkspaceRepository workspaceRepository;

    @Inject
    public WorkspaceDiff(
            final WorkspaceRepository workspaceRepository,
            final UserRepository userRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
        this.workspaceRepository = workspaceRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        String workspaceId = getActiveWorkspaceId(request);
        ClientApiWorkspaceDiff diff = handle(workspaceId, user);
        respondWithClientApiObject(response, diff);
    }

    public ClientApiWorkspaceDiff handle(String workspaceId, User user) {
        Workspace workspace = workspaceRepository.findById(workspaceId, true, user);
        if (workspace == null) {
            return null;
        }

        return this.workspaceRepository.getDiff(workspace, user);
    }
}
