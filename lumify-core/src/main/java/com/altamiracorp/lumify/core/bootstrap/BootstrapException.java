package com.altamiracorp.lumify.core.bootstrap;

/**
 * This exception is thrown when an error occurs during Lumify Bootstrapping.
 */
public class BootstrapException extends RuntimeException {
    /**
     * Create a new BootstrapException.
     * @param message the error message
     */
    public BootstrapException(final String message) {
        super(message);
    }

    /**
     * Create a new BootstrapException with a formatted error message.
     * @param messageFormat the message format, usable by String.format()
     * @param params the format parameters
     */
    public BootstrapException(final String messageFormat, final Object... params) {
        super(String.format(messageFormat, params));
    }
    
    /**
     * Create a new BootstrapException.
     * @param cause the cause of the exception
     */
    public BootstrapException(final Throwable cause) {
        super(cause);
    }
    
    /**
     * Create a new BootstrapException.
     * @param cause the cause of the exception
     * @param message the error message
     */
    public BootstrapException(final Throwable cause, final String message) {
        super(message, cause);
    }
    
    /**
     * Create a new BootstrapException with a cause and formatted error message.
     * @param cause the cause of the exception
     * @param messageFormat the message format, usable by String.format()
     * @param params the format parameters
     */
    public BootstrapException(final Throwable cause, final String messageFormat, final Object... params) {
        super(String.format(messageFormat, params), cause);
    }
}
