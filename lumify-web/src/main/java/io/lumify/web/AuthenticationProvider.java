package io.lumify.web;

import com.altamiracorp.miniweb.Handler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public abstract class AuthenticationProvider implements Handler {
    public static final String CURRENT_USER_REQ_ATTR_NAME = "user.current";

    public void setUserId(HttpServletRequest request, String userId) {
        request.getSession().setAttribute(AuthenticationProvider.CURRENT_USER_REQ_ATTR_NAME, userId);
    }

    public static String getUserId(HttpSession session) {
        if (session == null) {
            return null;
        }

        return (String) session.getAttribute(AuthenticationProvider.CURRENT_USER_REQ_ATTR_NAME);
    }

    public static String getUserId(HttpServletRequest request) {
        return getUserId(request.getSession());
    }

    public abstract boolean login(HttpServletRequest request);
}
