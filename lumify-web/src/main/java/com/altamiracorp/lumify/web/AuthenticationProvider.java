package com.altamiracorp.lumify.web;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.lumify.core.model.user.UserRow;
import com.altamiracorp.lumify.core.user.SystemUser;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.miniweb.Handler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public abstract class AuthenticationProvider implements Handler {
    public static final String CURRENT_USER_REQ_ATTR_NAME = "user.current";

    public void setUser(HttpServletRequest request, User user) {
        request.getSession().setAttribute(AuthenticationProvider.CURRENT_USER_REQ_ATTR_NAME, user);
    }

    public static User getUser(HttpSession session) {
        Object user = session.getAttribute(AuthenticationProvider.CURRENT_USER_REQ_ATTR_NAME);
        return user != null ? (User) user : null;
    }

    public static User getUser(HttpServletRequest request) {
        return AuthenticationProvider.getUser(request.getSession());
    }

    public User createFromModelUser(UserRow user) {
        // TODO change to user specific authorization
        ModelUserContext modelUserContext = SystemUser.getSystemUserContext();

        return new User(
                user.getRowKey().toString(),
                user.getMetadata().getUserName(),
                user.getMetadata().getCurrentWorkspace(),
                modelUserContext,
                user.getMetadata().getUserType());
    }
}
