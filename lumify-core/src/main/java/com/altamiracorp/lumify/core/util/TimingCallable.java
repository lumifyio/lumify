package com.altamiracorp.lumify.core.util;

import java.util.concurrent.Callable;

public abstract class TimingCallable<V> implements Callable<V> {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(TimingCallable.class);
    private final String message;

    public TimingCallable(String message) {
        this.message = message;
    }

    @Override
    public final V call() throws Exception {
        long startTime = System.currentTimeMillis();
        V result = callWithTime();
        long endTime = System.currentTimeMillis();
        LOGGER.debug("time for %s: %dms", message, endTime - startTime);
        return result;
    }

    protected abstract V callWithTime() throws Exception;
}
