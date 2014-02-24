package com.altamiracorp.lumify.core.exception;

import com.altamiracorp.lumify.core.user.User;

public class LumifyAccessDeniedException extends LumifyException {
    private final User user;
    private final Object resourceId;

    public LumifyAccessDeniedException(String message) {
        this(message, null, null);
    }

    public LumifyAccessDeniedException(String message, User user, Object resourceId) {
        super(message);
        this.user = user;
        this.resourceId = resourceId;
    }

    public User getUser() {
        return user;
    }

    public Object getResourceId() {
        return resourceId;
    }
}
