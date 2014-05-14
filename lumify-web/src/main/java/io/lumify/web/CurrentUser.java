package io.lumify.web;

import io.lumify.core.user.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class CurrentUser {
    public static final String CURRENT_USER_REQ_ATTR_NAME = "user.current";

    public static void set(HttpServletRequest request, String userId) {
        request.getSession().setAttribute(CurrentUser.CURRENT_USER_REQ_ATTR_NAME, userId);
    }

    public static String get(HttpSession session) {
        if (session == null) {
            return null;
        }

        return (String) session.getAttribute(CurrentUser.CURRENT_USER_REQ_ATTR_NAME);
    }

    public static String get(HttpServletRequest request) {
        return CurrentUser.get(request.getSession());
    }

    public static void clear(HttpServletRequest request) {
        request.getSession().removeAttribute(CurrentUser.CURRENT_USER_REQ_ATTR_NAME);
    }
}
