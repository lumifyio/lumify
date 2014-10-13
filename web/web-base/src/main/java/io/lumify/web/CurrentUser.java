package io.lumify.web;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class CurrentUser {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(CurrentUser.class);
    public static final String SESSION_ATTRIBUTE_NAME = "user.current";

    public static void set(HttpServletRequest request, String userId) {
        request.getSession().setAttribute(CurrentUser.SESSION_ATTRIBUTE_NAME, new SessionUser(userId));
    }

    public static String get(HttpSession session) {
        if (session == null) {
            LOGGER.warn("session is null");
            return null;
        }
        SessionUser sessionUser = (SessionUser) session.getAttribute(CurrentUser.SESSION_ATTRIBUTE_NAME);
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
        request.getSession().removeAttribute(CurrentUser.SESSION_ATTRIBUTE_NAME);
    }
}
