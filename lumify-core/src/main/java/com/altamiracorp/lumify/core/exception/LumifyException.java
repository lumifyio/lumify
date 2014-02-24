package com.altamiracorp.lumify.core.exception;

public class LumifyException extends RuntimeException {
    public LumifyException(String message) {
        super(message);
    }

    public LumifyException(String message, Throwable cause) {
        super(message, cause);
    }
}
