package com.altamiracorp.lumify.web;

import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.miniweb.utils.UrlUtils;
import com.google.inject.Inject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UsernameAndPasswordAuthenticationProvider extends AuthenticationProvider {
    private final UserRepository userRepository;

    @Inject
    public UsernameAndPasswordAuthenticationProvider(final UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        if (user != null) {
            chain.next(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    @Override
    public boolean login(HttpServletRequest request) {
        final String username = UrlUtils.urlDecode(request.getParameter("username"));
        final String password = UrlUtils.urlDecode(request.getParameter("password"));

        User user = userRepository.findByDisplayName(username);
        if (user != null && userRepository.isPasswordValid(user, password)) {
            setUser(request, user);
            return true;
        } else {
            return false;
        }
    }
}
