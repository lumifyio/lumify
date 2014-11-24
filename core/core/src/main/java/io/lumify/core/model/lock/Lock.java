package io.lumify.core.model.lock;

import org.apache.curator.framework.recipes.locks.InterProcessLock;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class Lock {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(Lock.class);
    private final InterProcessLock lock;
    private final String lockName;

    public Lock(InterProcessLock lock, String lockName) {
        this.lock = lock;
        this.lockName = lockName;
    }

    public <T> T run(Callable<T> runnable) {
        try {
            LOGGER.debug("acquire lock: %s", this.lockName);
            if (!this.lock.acquire(30, TimeUnit.SECONDS)) {
                throw new LumifyException("Could not acquire lock " + lockName);
            }
            LOGGER.debug("acquired lock: %s", this.lockName);
            try {
                return runnable.call();
            } finally {
                this.lock.release();
                LOGGER.debug("released lock: %s", this.lockName);
            }
        } catch (Exception ex) {
            throw new LumifyException("Failed to run in lock", ex);
        }
    }
}
