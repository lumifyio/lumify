package com.altamiracorp.lumify.web;

import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.miniweb.HandlerChain;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SessionAuthenticationProvider extends AuthenticationProvider {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        if (user != null) {
            chain.next(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }
}
