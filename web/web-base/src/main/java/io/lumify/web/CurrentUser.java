package io.lumify.web;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class CurrentUser {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(CurrentUser.class);
    public static final String SESSIONUSER_ATTRIBUTE_NAME = "user.current";
    public static final String STRING_ATTRIBUTE_NAME = "username";

    public static void set(HttpServletRequest request, String userId, String userName) {
        request.getSession().setAttribute(CurrentUser.SESSIONUSER_ATTRIBUTE_NAME, new SessionUser(userId));
        request.getSession().setAttribute(CurrentUser.STRING_ATTRIBUTE_NAME, userName);
    }

    public static String get(HttpSession session) {
        if (session == null) {
            LOGGER.warn("session is null");
            return null;
        }
        SessionUser sessionUser = (SessionUser) session.getAttribute(CurrentUser.SESSIONUSER_ATTRIBUTE_NAME);
        if (sessionUser == null) {
            LOGGER.warn("sessionUser is null");
            return null;
        }
        return sessionUser.getUserId();
    }

    public static String get(HttpServletRequest request) {
        return CurrentUser.get(request.getSession());
    }

    public static void clear(HttpServletRequest request) {
        request.getSession().removeAttribute(CurrentUser.SESSIONUSER_ATTRIBUTE_NAME);
        request.getSession().removeAttribute(CurrentUser.STRING_ATTRIBUTE_NAME);
    }
}
