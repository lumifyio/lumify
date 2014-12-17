package io.lumify.bigtable.model.notification;

import com.altamiracorp.bigtable.model.FlushFlag;
import com.altamiracorp.bigtable.model.ModelSession;
import com.google.inject.Inject;
import io.lumify.bigtable.model.notification.model.SystemNotificationRowKey;
import io.lumify.core.model.lock.LockRepository;
import io.lumify.core.model.notification.SystemNotification;
import io.lumify.core.model.notification.SystemNotificationRepository;
import io.lumify.core.model.notification.SystemNotificationSeverity;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class BigTableSystemNotificationRepository extends SystemNotificationRepository {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(BigTableSystemNotificationRepository.class);
    private io.lumify.bigtable.model.notification.model.SystemNotificationRepository repository;

    @Inject
    public BigTableSystemNotificationRepository(ModelSession modelSession,
                                                LockRepository lockRepository,
                                                UserRepository userRepository,
                                                WorkQueueRepository workQueueRepository) {
        repository = new io.lumify.bigtable.model.notification.model.SystemNotificationRepository(modelSession);
        startBackgroundThread(lockRepository, userRepository, workQueueRepository);
    }

    @Override
    public List<SystemNotification> getActiveNotifications(User user) {
        Date now = new Date();
        List<SystemNotification> activeNotifications = new ArrayList<SystemNotification>();
        for (SystemNotification notification : repository.findAll(user.getModelUserContext())) {
            if (notification.getStartDate().before(now)) {
                if (notification.getEndDate() == null || notification.getEndDate().after(now)) {
                    activeNotifications.add(notification);
                }
            }
        }
        LOGGER.debug("returning %d active system notifications", activeNotifications.size());
        return activeNotifications;
    }

    @Override
    public List<SystemNotification> getFutureNotifications(Date maxDate, User user) {
        Date now = new Date();
        List<SystemNotification> futureNotifications = new ArrayList<SystemNotification>();
        for (SystemNotification notification : repository.findAll(user.getModelUserContext())) {
            if (notification.getStartDate().after(now) && notification.getStartDate().before(maxDate)) {
                futureNotifications.add(notification);
            }
        }
        LOGGER.debug("returning %d future system notifications", futureNotifications.size());
        return futureNotifications;
    }

    @Override
    public SystemNotification getNotification(String rowKey, User user) {
        return repository.findByRowKey(rowKey, user.getModelUserContext());
    }

    @Override
    public BigTableSystemNotification createNotification(SystemNotificationSeverity severity, String title, String message, Date startDate, Date endDate) {
        if (startDate == null) {
            startDate = new Date();
        }
        String rowKey = Long.toString(startDate.getTime()) + ":" + UUID.randomUUID().toString();
        BigTableSystemNotification notification = new BigTableSystemNotification(new SystemNotificationRowKey(rowKey));
        notification.setSeverity(severity);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setStartDate(startDate);
        notification.setEndDate(endDate);
        repository.save(notification, FlushFlag.FLUSH);
        return notification;
    }

    @Override
    public SystemNotification updateNotification(SystemNotification notification) {
        repository.save((BigTableSystemNotification) notification, FlushFlag.FLUSH);
        return notification;
    }

    @Override
    public void endNotification(SystemNotification notification) {
        repository.delete(((BigTableSystemNotification) notification).getRowKey());
    }
}