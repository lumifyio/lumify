package com.altamiracorp.lumify.web.routes.user;

import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.model.user.UserRow;
import com.altamiracorp.lumify.core.user.SystemUser;
import com.altamiracorp.lumify.web.AuthenticationProvider;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Login extends BaseRequestHandler {
    private final UserRepository userRepository;
    private final AuthenticationProvider authenticationProvider;

    @Inject
    public Login(final UserRepository userRepository, final AuthenticationProvider authenticationProvider) {
        this.userRepository = userRepository;
        this.authenticationProvider = authenticationProvider;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String username = getRequiredParameter(request, "username");
        final String password = getRequiredParameter(request, "password");

        UserRow user = userRepository.findByUserName(username, new SystemUser());
        if (user != null && user.isPasswordValid(password)) {
            authenticationProvider.setUser(request, authenticationProvider.createFromModelUser(user));

            JSONObject json = new JSONObject();
            json.put("rowkey", user.getRowKey());
            respondWithJson(response, json);
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }
}
