package io.lumify.core.model.lock;

import java.util.concurrent.Callable;

public class LocalLockRepository extends LockRepository {
    public LocalLockRepository() {
        super(null);
    }

    @Override
    public Lock createLock(String lockName) {
        return new Lock(null, lockName) {
            @Override
            public <T> T run(Callable<T> runnable) {
                try {
                    return runnable.call();
                } catch (Exception ex) {
                    throw new RuntimeException("Failed to run in lock", ex);
                }
            }
        };
    }
}
