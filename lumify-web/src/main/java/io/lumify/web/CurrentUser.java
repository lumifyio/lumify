package io.lumify.web;

import io.lumify.core.user.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class CurrentUser {
    public static final String CURRENT_USER_REQ_ATTR_NAME = "user.current";

    public static void set(HttpServletRequest request, User user) {
        request.getSession().setAttribute(CurrentUser.CURRENT_USER_REQ_ATTR_NAME, user);
    }

    public static User get(HttpSession session) {
        if (session == null) {
            return null;
        }

        Object user = session.getAttribute(CurrentUser.CURRENT_USER_REQ_ATTR_NAME);
        return user != null ? (User) user : null;
    }

    public static User get(HttpServletRequest request) {
        return CurrentUser.get(request.getSession());
    }
}
