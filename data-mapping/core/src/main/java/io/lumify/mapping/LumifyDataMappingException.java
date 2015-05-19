package io.lumify.mapping;

import io.lumify.core.exception.LumifyException;

/**
 * Exception thrown when errors occur while a document mapping is applied.
 */
public class LumifyDataMappingException extends LumifyException {
    public LumifyDataMappingException(String message) {
        super(message);
    }

    public LumifyDataMappingException(String message, Throwable cause) {
        super(message, cause);
    }
}
