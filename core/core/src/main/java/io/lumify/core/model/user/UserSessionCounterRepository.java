package io.lumify.core.model.user;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.atomic.AtomicValue;
import org.apache.curator.framework.recipes.atomic.DistributedAtomicInteger;
import org.apache.curator.retry.BoundedExponentialBackoffRetry;

public class UserSessionCounterRepository {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(UserSessionCounterRepository.class);
    public static final String DEFAULT_PATH_PREFIX = "/lumify/userSessionCounters/";
    private static final int BASE_SLEEP_TIME_MS = 10;
    private static final int MAX_SLEEP_TIME_MS = 2000;
    private static final int MAX_RETRIES = 5;
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

        int count = adjustAndGet(userId, Direction.INCREMENT);

        LOGGER.debug("user session counter for %s is now %d", userId, count);
        return count;
    }

    public int decrementAndGet(String userId) {
        LOGGER.debug("decrementing user session counter for %s", userId);

        int count = adjustAndGet(userId, Direction.DECREMENT);
        if (count < 1) {
            removeCounter(userId);
        }

        LOGGER.debug("user session counter for %s is now %d", userId, count);
        return count;
    }

    private int adjustAndGet(String userId, Direction direction) {
        RetryPolicy retryPolicy = new BoundedExponentialBackoffRetry(BASE_SLEEP_TIME_MS, MAX_SLEEP_TIME_MS, MAX_RETRIES);
        DistributedAtomicInteger distributedAtomicInteger = new DistributedAtomicInteger(curatorFramework, counterPath(userId), retryPolicy);

        try {
            distributedAtomicInteger.initialize(0); // this will respect an existing value but set uninitialized values to 0
        } catch (Exception e) {
            throw new LumifyException("failed to initialize counter for " + userId);
        }

        try {
            AtomicValue<Integer> count = direction == Direction.INCREMENT ? distributedAtomicInteger.increment() : distributedAtomicInteger.decrement();
            if (count.succeeded()) {
                return count.postValue();
            } else {
                throw new LumifyException("failed to " + direction + " counter for " + userId);
            }
        } catch (Exception e) {
            throw new LumifyException("failed to " + direction + " counter for " + userId, e);
        }
    }

    private void removeCounter(String userId) {
        LOGGER.debug("removing user session counter for %s", userId);
        try {
            curatorFramework.delete().inBackground().forPath(counterPath(userId));
        } catch (Exception e) {
            throw new LumifyException("failed to remove user session counter for " + userId, e);
        }
    }

    private String counterPath(String userId) {
        return pathPrefix + userId;
    }

    public enum Direction {
        INCREMENT,
        DECREMENT
    }
}
