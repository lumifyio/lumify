/*
 * Copyright 2014 Altamira Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
