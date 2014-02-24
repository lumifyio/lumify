package com.altamiracorp.lumify.core.exception;

public class LumifyResourceNotFoundException extends LumifyException {
    private final Object resourceId;

    public LumifyResourceNotFoundException(String message) {
        this(message, null);
    }

    public LumifyResourceNotFoundException(String message, Object resourceId) {
        super(message);
        this.resourceId = resourceId;
    }

    public Object getResourceId() {
        return resourceId;
    }
}
