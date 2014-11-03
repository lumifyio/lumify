package io.lumify.core.model.user;

import com.google.inject.Inject;
import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.framework.recipes.shared.SharedCount;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;

import java.io.IOException;

public class UserSessionCounterRepository {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(UserSessionCounterRepository.class);
    public static final String DEFAULT_PATH_PREFIX = "/lumify/userSessionCounters/";
    private static final int DELAY_MULTIPLIER_MS = 10;
    private static final int MAX_ATTEMPTS = 10;
    private final CuratorFramework curatorFramework;
    private final String pathPrefix;

    @Inject
    public UserSessionCounterRepository(final CuratorFramework curatorFramework,
                                        final Configuration configuration) {
        this.curatorFramework = curatorFramework;
        this.pathPrefix = configuration.get(Configuration.USER_SESSION_COUNTER_PATH_PREFIX, DEFAULT_PATH_PREFIX);
    }

    public int incrementAndGet(String userId) {
        LOGGER.debug("incrementing user session counter for %s", userId);

        int count = adjustAndGet(userId, 1);

        LOGGER.debug("user session counter for %s is now %d", userId, count);
        return count;
    }

    public int decrementAndGet(String userId) {
        LOGGER.debug("decrementing user session counter for %s", userId);

        int count = adjustAndGet(userId, -1);
        if (count < 1) {
            remove(userId);
        }

        LOGGER.debug("user session counter for %s is now %d", userId, count);
        return count;
    }

    private int adjustAndGet(String userId, int adjustment) {
        SharedCount sharedCount = new SharedCount(curatorFramework, pathPrefix + userId, 0);

        try {
            sharedCount.start();
        } catch (Exception e) {
            throw new LumifyException("failed to start user session counter for " + userId, e);
        }

        int count = 0;
        int attempt = 0;
        boolean success = false;
        while (!success && attempt < MAX_ATTEMPTS) {
            count = sharedCount.getCount() + adjustment;
            try {
                success = sharedCount.trySetCount(count);
            } catch (Exception e) {
                throw new LumifyException("failed to adjust (" + adjustment + ") user session counter for " + userId, e);
            }
            if (!success) {
                int delay = (2 ^ attempt) * DELAY_MULTIPLIER_MS;
                attempt = attempt + 1;
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    // do nothing
                }
            }
        }
        if (!success) {
            throw new LumifyException("failed to adjust (" + adjustment + ") user session counter for " + userId + " within " + MAX_ATTEMPTS + " attempts");
        }

        try {
            sharedCount.close();
        } catch (IOException e) {
            throw new LumifyException("failed to close user session counter for " + userId, e);
        }

        return count;
    }

    private void remove(String userId) {
        LOGGER.debug("removing user session counter for %s", userId);
        try {
            curatorFramework.delete().inBackground().forPath(pathPrefix + userId);
        } catch (Exception e) {
            throw new LumifyException("failed to remove user session counter for " + userId, e);
        }
    }
}
