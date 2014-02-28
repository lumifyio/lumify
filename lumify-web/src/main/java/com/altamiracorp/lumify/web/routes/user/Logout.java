package com.altamiracorp.lumify.web.routes.user;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.web.AuthenticationProvider;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Logout extends BaseRequestHandler {
    private final AuthenticationProvider authenticationProvider;

    @Inject
    public Logout(
            final AuthenticationProvider authenticationProvider,
            final UserRepository userRepository,
            final Configuration configuration) {
        super(userRepository, configuration);
        this.authenticationProvider = authenticationProvider;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        this.authenticationProvider.setUser(request, null);
        JSONObject json = new JSONObject();
        json.put("status", "ok");
        respondWithJson(response, json);
    }
}
