package io.lumify.web.devTools.user;

import io.lumify.miniweb.HandlerChain;
import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.Workspace;
import io.lumify.web.clientapi.model.WorkspaceAccess;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.web.BaseRequestHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WorkspaceShareWithMe extends BaseRequestHandler {
    @Inject
    public WorkspaceShareWithMe(
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String workspaceId = getRequiredParameter(request, "workspaceId");
        String userName = getRequiredParameter(request, "user-name");

        User me = getUser(request);

        User user = this.getUserRepository().findByUsername(userName);
        if (user == null) {
            respondWithNotFound(response);
            return;
        }

        Workspace workspace = getWorkspaceRepository().findById(workspaceId, user);
        if (workspace == null) {
            respondWithNotFound(response);
            return;
        }

        getWorkspaceRepository().updateUserOnWorkspace(workspace, me.getUserId(), WorkspaceAccess.WRITE, user);

        JSONObject json = new JSONObject();
        respondWithJson(response, json);
    }
}
