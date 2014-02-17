package com.altamiracorp.lumify.web.routes.user;

import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Vertex;
import com.google.inject.Inject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MeGet extends BaseRequestHandler {

    private final UserRepository userRepository;

    @Inject
    public MeGet(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        Vertex userVertex = userRepository.findByUserName(user.getUsername());
        if (userVertex == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        respondWithJson(response, UserRepository.toJson(userVertex));
    }
}
