package com.altamiracorp.lumify.web.routes.user;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Vertex;
import com.google.inject.Inject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UserRemoveAuthorization extends BaseRequestHandler {
    @Inject
    public UserRemoveAuthorization(
            final UserRepository userRepository,
            final Configuration configuration) {
        super(userRepository, configuration);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String auth = getRequiredParameter(request, "auth");

        User user = getUser(request);
        if (user == null) {
            respondWithNotFound(response);
            return;
        }

        getUserRepository().removeAuthorization(user, auth);

        respondWithJson(response, getUserRepository().toJsonWithAuths(user));
    }
}
