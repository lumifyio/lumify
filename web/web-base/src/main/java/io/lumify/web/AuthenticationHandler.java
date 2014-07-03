package io.lumify.web;

import com.altamiracorp.miniweb.Handler;
import com.altamiracorp.miniweb.HandlerChain;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AuthenticationHandler implements Handler {

    public static final String LOGIN_PATH = "/login";

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        if (CurrentUser.get(request) != null) {
            chain.next(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }
}
