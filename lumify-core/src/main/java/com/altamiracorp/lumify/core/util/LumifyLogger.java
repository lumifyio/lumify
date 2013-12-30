package com.altamiracorp.lumify.core.util;

import org.slf4j.Logger;

public class LumifyLogger {
    private final Logger logger;

    public LumifyLogger(Logger logger) {
        this.logger = logger;
    }

    public void trace(String format, Object... args) {
        if (isDebugEnabled()) {
            this.logger.trace(format(format, args));
        }
    }

    public void trace(String message, Throwable t) {
        if (isDebugEnabled()) {
            this.logger.trace(message, t);
        }
    }

    public void debug(String format, Object... args) {
        if (isDebugEnabled()) {
            this.logger.debug(format(format, args));
        }
    }

    public void debug(String message, Throwable t) {
        if (isDebugEnabled()) {
            this.logger.debug(message, t);
        }
    }

    public void info(String format, Object... args) {
        if (isInfoEnabled()) {
            this.logger.info(format(format, args));
        }
    }

    public void info(String message, Throwable t) {
        if (isInfoEnabled()) {
            this.logger.info(message, t);
        }
    }

    public void warn(String format, Object... args) {
        if (this.logger.isWarnEnabled()) {
            this.logger.warn(format(format, args));
        }
    }

    public void warn(String message, Throwable t) {
        if (this.logger.isWarnEnabled()) {
            this.logger.warn(message, t);
        }
    }

    public void error(String format, Object... args) {
        if (this.logger.isErrorEnabled()) {
            this.logger.error(format(format, args));
        }
    }

    public void error(String message, Throwable t) {
        if (this.logger.isErrorEnabled()) {
            this.logger.error(message, t);
        }
    }

    public boolean isDebugEnabled() {
        return this.logger.isDebugEnabled();
    }

    public boolean isInfoEnabled() {
        return this.logger.isInfoEnabled();
    }

    private String format(String format, Object[] args) {
        try {
            return String.format(format, args);
        } catch (Exception ex) {
            error("Invalid format string: " + format, ex);
            StringBuilder sb = new StringBuilder();
            sb.append(format);
            for (Object arg : args) {
                sb.append(", ");
                sb.append(arg);
            }
            return sb.toString();
        }
    }
}
