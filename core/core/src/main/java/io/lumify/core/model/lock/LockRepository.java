package io.lumify.core.model.lock;

import com.google.inject.Inject;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import io.lumify.core.config.Configuration;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class LockRepository {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(LockRepository.class);
    public static final String DEFAULT_PATH_PREFIX = "/lumify/locks";
    private final CuratorFramework curatorFramework;
    private final String pathPrefix;
    private final Map<String, Object> localLocks = new HashMap<String, Object>();

    @Inject
    public LockRepository(final CuratorFramework curatorFramework,
                          final Configuration configuration) {
        this.curatorFramework = curatorFramework;
        this.pathPrefix = configuration.get(Configuration.LOCK_REPOSITORY_PATH_PREFIX, DEFAULT_PATH_PREFIX);
    }

    public Lock createLock(String lockName) {
        InterProcessLock l = new InterProcessMutex(this.curatorFramework, getPath(lockName));
        return new Lock(l, lockName);
    }

    private String getPath(String lockName) {
        return this.pathPrefix + "/" + lockName;
    }

    public void lock(String lockName, final Runnable runnable) {
        lock(lockName, new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                runnable.run();
                return null;
            }
        });
    }

    public <T> T lock(String lockName, Callable<T> callable) {
        LOGGER.debug("starting lock: %s", lockName);
        try {
            Object localLock = getLocalLock(lockName);
            synchronized (localLock) {
                Lock lock = createLock(lockName);
                return lock.run(callable);
            }
        } finally {
            LOGGER.debug("ending lock: %s", lockName);
        }
    }

    private Object getLocalLock(String lockName) {
        synchronized (localLocks) {
            Object localLock = localLocks.get(lockName);
            if (localLock == null) {
                localLock = new Object();
                localLocks.put(lockName, localLock);
            }
            return localLock;
        }
    }
}
