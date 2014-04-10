package com.altamiracorp.lumify.web.routes.user;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UserInfo extends BaseRequestHandler {
    @Inject
    public UserInfo(
            final UserRepository userRepository,
            final Configuration configuration) {
        super(userRepository, configuration);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String userId = getRequiredParameter(request, "userId");

        User user = getUserRepository().findById(userId);
        if (user == null) {
            respondWithNotFound(response, "Could not find user with id: " + userId);
            return;
        }

        JSONObject json = getUserRepository().toJson(user);
        respondWithJson(response, json);
    }
}
