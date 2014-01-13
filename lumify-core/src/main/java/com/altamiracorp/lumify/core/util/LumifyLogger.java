package com.altamiracorp.lumify.core.util;

import org.slf4j.Logger;

public class LumifyLogger {
    private final Logger logger;

    public LumifyLogger(final Logger logger) {
        this.logger = logger;
    }

    public void trace(final String format, final Object... args) {
        if (isDebugEnabled()) {
            logger.trace(format(format, args), findLastThrowable(args));
        }
    }

    public void trace(final String message, final Throwable t) {
        if (isDebugEnabled()) {
            logger.trace(message, t);
        }
    }

    public void debug(final String format, final Object... args) {
        if (isDebugEnabled()) {
            logger.debug(format(format, args), findLastThrowable(args));
        }
    }

    public void debug(final String message, final Throwable t) {
        if (isDebugEnabled()) {
            logger.debug(message, t);
        }
    }

    public void info(final String format, final Object... args) {
        if (isInfoEnabled()) {
            logger.info(format(format, args), findLastThrowable(args));
        }
    }

    public void info(final String message, final Throwable t) {
        if (isInfoEnabled()) {
            logger.info(message, t);
        }
    }

    public void warn(final String format, final Object... args) {
        if (logger.isWarnEnabled()) {
            logger.warn(format(format, args), findLastThrowable(args));
        }
    }

    public void warn(final String message, final Throwable t) {
        if (logger.isWarnEnabled()) {
            logger.warn(message, t);
        }
    }

    public void error(final String format, final Object... args) {
        if (logger.isErrorEnabled()) {
            logger.error(format(format, args), findLastThrowable(args));
        }
    }

    public void error(final String message, final Throwable t) {
        if (logger.isErrorEnabled()) {
            logger.error(message, t);
        }
    }

    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    private String format(final String format, final Object[] args) {
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
    
    private Throwable findLastThrowable(final Object[] args) {
        int length = args != null ? args.length : 0;
        return (length > 0 && args[length-1] instanceof Throwable) ? (Throwable) args[length-1] : null;
    }
}
