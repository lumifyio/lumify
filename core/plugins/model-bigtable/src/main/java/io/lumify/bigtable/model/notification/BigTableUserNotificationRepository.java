package io.lumify.bigtable.model.notification;

import com.altamiracorp.bigtable.model.FlushFlag;
import com.altamiracorp.bigtable.model.ModelSession;
import com.google.inject.Inject;
import io.lumify.bigtable.model.notification.model.SystemNotificationRowKey;
import io.lumify.bigtable.model.notification.model.UserNotificationRowKey;
import io.lumify.core.model.lock.LockRepository;
import io.lumify.core.model.notification.*;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

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
            if (notification.getSentDate().before(now)) {
                if (notification.isActive()) {
                    activeNotifications.add(notification);
                }
            }
        }
        LOGGER.debug("returning %d active user notifications", activeNotifications.size());
        return activeNotifications;
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
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setSentDate(now);
        if (expirationAge != null) {
            notification.setExpirationAge(expirationAge);
        }
        repository.save(notification, FlushFlag.FLUSH);
        workQueueRepository.pushUserNotification(notification);
        return notification;
    }

    @Override
    public UserNotification updateNotification(UserNotification notification) {
        repository.save((BigTableUserNotification) notification, FlushFlag.FLUSH);
        return notification;
    }

    @Override
    public void endNotification(UserNotification notification) {
        repository.delete(((BigTableUserNotification) notification).getRowKey());
    }
}