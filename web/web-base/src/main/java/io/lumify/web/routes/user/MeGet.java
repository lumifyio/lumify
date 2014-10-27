package io.lumify.web.routes.user;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.miniweb.HandlerChain;
import io.lumify.miniweb.handlers.CSRFHandler;
import io.lumify.web.BaseRequestHandler;
import io.lumify.web.clientapi.model.ClientApiUser;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MeGet extends BaseRequestHandler {
    @Inject
    public MeGet(
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        if (user == null || user.getUsername() == null) {
            respondWithNotFound(response);
            return;
        }

        ClientApiUser userMe = getUserRepository().toClientApiPrivate(user);
        userMe.setCsrfToken(CSRFHandler.getSavedToken(request, true));

        respondWithClientApiObject(response, userMe);
    }
}
