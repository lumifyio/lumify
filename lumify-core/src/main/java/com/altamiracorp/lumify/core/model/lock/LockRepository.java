package com.altamiracorp.lumify.core.model.lock;

import com.google.inject.Inject;
import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.framework.recipes.locks.InterProcessLock;
import com.netflix.curator.framework.recipes.locks.InterProcessSemaphoreMutex;

public class LockRepository {
    public static final String DEFAULT_PATH_PREFIX = "/lumify/locks/";
    private final CuratorFramework curatorFramework;
    private final String pathPrefix;

    @Inject
    public LockRepository(final CuratorFramework curatorFramework) {
        this.curatorFramework = curatorFramework;
        this.pathPrefix = DEFAULT_PATH_PREFIX;
    }

    public Lock createLock(String lockName) {
        InterProcessLock l = new InterProcessSemaphoreMutex(this.curatorFramework, getPath(lockName));
        return new Lock(l);
    }

    private String getPath(String lockName) {
        return this.pathPrefix + lockName;
    }
}
