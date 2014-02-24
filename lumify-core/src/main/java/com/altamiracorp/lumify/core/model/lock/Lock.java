package com.altamiracorp.lumify.core.model.lock;

import com.netflix.curator.framework.recipes.locks.InterProcessLock;

public class Lock {
    private final InterProcessLock lock;

    public Lock(InterProcessLock lock) {
        this.lock = lock;
    }

    public void run(Runnable runnable) {
        try {
            this.lock.acquire();
            try {
                runnable.run();
            } finally {
                this.lock.release();
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to run in lock", ex);
        }
    }
}
