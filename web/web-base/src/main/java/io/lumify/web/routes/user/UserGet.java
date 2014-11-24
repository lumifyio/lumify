package io.lumify.web.routes.user;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.Workspace;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import io.lumify.web.clientapi.model.ClientApiUser;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UserGet extends BaseRequestHandler {
    @Inject
    public UserGet(
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String userName = getRequiredParameter(request, "user-name");

        User user = this.getUserRepository().findByUsername(userName);
        if (user == null) {
            respondWithNotFound(response);
            return;
        }

        ClientApiUser clientApiUser = getUserRepository().toClientApiPrivate(user);

        Iterable<Workspace> workspaces = getWorkspaceRepository().findAll(user);
        for (Workspace workspace : workspaces) {
            clientApiUser.getWorkspaces().add(getWorkspaceRepository().toClientApi(workspace, user, false));
        }

        respondWithClientApiObject(response, clientApiUser);
    }
}
