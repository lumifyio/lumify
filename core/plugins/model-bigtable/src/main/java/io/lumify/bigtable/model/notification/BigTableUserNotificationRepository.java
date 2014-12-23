package io.lumify.bigtable.model.notification;

import com.altamiracorp.bigtable.model.FlushFlag;
import com.altamiracorp.bigtable.model.ModelSession;
import com.google.inject.Inject;
import io.lumify.bigtable.model.notification.model.UserNotificationRowKey;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.notification.ExpirationAge;
import io.lumify.core.model.notification.UserNotification;
import io.lumify.core.model.notification.UserNotificationRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;

import java.util.*;

public class BigTableUserNotificationRepository extends UserNotificationRepository {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(BigTableUserNotificationRepository.class);
    private io.lumify.bigtable.model.notification.model.UserNotificationRepository repository;
    private final WorkQueueRepository workQueueRepository;

    @Inject
    public BigTableUserNotificationRepository(ModelSession modelSession, WorkQueueRepository workQueueRepository) {
        repository = new io.lumify.bigtable.model.notification.model.UserNotificationRepository(modelSession);
        this.workQueueRepository = workQueueRepository;
    }

    @Override
    public List<UserNotification> getActiveNotifications(User user) {
        Date now = new Date();
        List<UserNotification> activeNotifications = new ArrayList<UserNotification>();
        for (UserNotification notification : repository.findAll(user.getModelUserContext())) {
            if (user.getUserId().equals(notification.getUserId()) &&
                    notification.getSentDate().before(now) &&
                    notification.isActive()) {
                activeNotifications.add(notification);
            }
        }
        LOGGER.debug("returning %d active user notifications", activeNotifications.size());
        return activeNotifications;
    }

    @Override
    public void markRead(String[] rowKeys, User user) {
        Collection<BigTableUserNotification> toSave = new ArrayList<BigTableUserNotification>();
        for (String rowKey : rowKeys) {
            UserNotification notification = getNotification(rowKey, user);
            if (notification.getUserId().equals(user.getUserId())) {
                notification.setMarkedRead(true);
                toSave.add((BigTableUserNotification) notification);
            } else throw new LumifyException("User cannot mark notifications read that aren't issued to them");
        }
        repository.saveMany(toSave);
    }

    @Override
    public UserNotification getNotification(String rowKey, User user) {
        return repository.findByRowKey(rowKey, user.getModelUserContext());
    }

    @Override
    public BigTableUserNotification createNotification(String user, String title, String message, ExpirationAge expirationAge) {
        Date now = new Date();
        String rowKey = Long.toString(now.getTime()) + ":" + UUID.randomUUID().toString();
        BigTableUserNotification notification = new BigTableUserNotification(new UserNotificationRowKey(rowKey));
        notification.setUserId(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setSentDate(now);
        notification.setMarkedRead(false);
        if (expirationAge != null) {
            notification.setExpirationAge(expirationAge);
        }
        repository.save(notification, FlushFlag.FLUSH);
        workQueueRepository.pushUserNotification(notification);
        return notification;
    }
}