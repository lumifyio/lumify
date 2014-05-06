package io.lumify.web;

import com.altamiracorp.miniweb.Handler;
import com.altamiracorp.miniweb.HandlerChain;
import io.lumify.core.user.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AuthenticationHandler implements Handler {

    public static final String LOGIN_PATH = "/login";
    public static final String LOGOUT_PATH = "/logout";

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = CurrentUser.get(request);
        if (user != null) {
            chain.next(request, response);
        } else {
            if (isAjaxRequest(request)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            } else {
                response.sendRedirect(LOGIN_PATH);
            }
        }
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        String ajaxHeader = request.getHeader("X-Requested-With");
        return ajaxHeader != null && ajaxHeader.toLowerCase().equals("xmlhttprequest");
    }
}
