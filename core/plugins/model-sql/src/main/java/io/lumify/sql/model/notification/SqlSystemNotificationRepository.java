package io.lumify.sql.model.notification;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.lumify.core.model.lock.LockRepository;
import io.lumify.core.model.notification.SystemNotification;
import io.lumify.core.model.notification.SystemNotificationRepository;
import io.lumify.core.model.notification.SystemNotificationSeverity;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.sql.model.HibernateSessionManager;
import org.hibernate.Session;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Singleton
public class SqlSystemNotificationRepository extends SystemNotificationRepository {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(SqlSystemNotificationRepository.class);
    private final HibernateSessionManager sessionManager;

    @Inject
    public SqlSystemNotificationRepository(HibernateSessionManager sessionManager,
                                           LockRepository lockRepository,
                                           UserRepository userRepository,
                                           WorkQueueRepository workQueueRepository) {
        this.sessionManager = sessionManager;
        startBackgroundThread(lockRepository, userRepository, workQueueRepository);
    }

    @Override
    public List<SystemNotification> getActiveNotifications(User user) {
        Session session = sessionManager.getSession();
        List<SystemNotification> activeNotifications = session.createQuery(
                "select sn from " + SqlSystemNotification.class.getSimpleName() + " as sn where sn.startDate <= :now and (sn.endDate is null or sn.endDate > :now)")
                .setParameter("now", new Date())
                .list();
        LOGGER.debug("returning %d active system notifications", activeNotifications.size());
        return activeNotifications;
    }

    @Override
    public List<SystemNotification> getFutureNotifications(Date maxDate, User user) {
       Session session = sessionManager.getSession();
        List<SystemNotification> futureNotifications = session.createQuery(
                "select sn from " + SqlSystemNotification.class.getSimpleName() + " as sn where sn.startDate > :now and sn.startDate < :maxDate")
                .setParameter("now", new Date())
                .setParameter("maxDate", maxDate)
                .list();
        LOGGER.debug("returning %d future system notifications", futureNotifications.size());
        return futureNotifications;
    }

    @Override
    public SystemNotification createNotification(SystemNotificationSeverity severity, String title, String message, String actionEvent, JSONObject actionPayload, Date startDate, Date endDate) {
        if (startDate == null) {
            startDate = new Date();
        }
        String id = Long.toString(startDate.getTime()) + ":" + UUID.randomUUID().toString();
        Session session = sessionManager.getSession();
        SqlSystemNotification notification = new SqlSystemNotification();
        notification.setId(id);
        notification.setSeverity(severity);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setStartDate(startDate);
        notification.setEndDate(endDate);
        if (actionEvent != null) {
            notification.setActionEvent(actionEvent);
        }
        if (actionPayload != null) {
            notification.setActionPayload(actionPayload);
        }
        session.save(notification);
        return notification;
    }

    @Override
    public SystemNotification updateNotification(SystemNotification notification) {
        Session session = sessionManager.getSession();
        session.update(notification);
        return notification;
    }

    @Override
    public SystemNotification getNotification(String rowKey, User user) {
        Session session = sessionManager.getSession();
        return (SystemNotification) session.byId(rowKey).getReference(SystemNotification.class);
    }

    @Override
    public void endNotification(SystemNotification notification) {
        Session session = sessionManager.getSession();
        notification.setEndDate(new Date());
        session.update(notification);
    }
}
