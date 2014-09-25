package io.lumify.web.clientapi;

public class LumifyClientApiException extends RuntimeException {
    public LumifyClientApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public LumifyClientApiException(String message) {
        super(message);
    }
}
