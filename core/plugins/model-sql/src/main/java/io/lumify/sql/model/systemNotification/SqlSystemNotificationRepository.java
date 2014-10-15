package io.lumify.sql.model.systemNotification;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.lumify.core.model.systemNotification.SystemNotification;
import io.lumify.core.model.systemNotification.SystemNotificationRepository;
import io.lumify.core.model.systemNotification.SystemNotificationSeverity;
import io.lumify.sql.model.HibernateSessionManager;
import org.hibernate.Session;

import java.util.Date;
import java.util.List;

@Singleton
public class SqlSystemNotificationRepository extends SystemNotificationRepository {
    private final HibernateSessionManager sessionManager;

    @Inject
    public SqlSystemNotificationRepository(HibernateSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public List<SystemNotification> getActiveNotifications() {
        Session session = sessionManager.getSession();
        return session.createQuery(
                "select * from " + SqlSystemNotification.class.getSimpleName() + " as sn where sn.start_date <= :now and (sn.end_date is null or sn.end_date > :now")
                .setParameter("now", new Date())
                .list();
    }

    @Override
    public List<SystemNotification> getFutureNotifications(Date maxDate) {
       Session session = sessionManager.getSession();
        return session.createQuery(
                "select * from " + SqlSystemNotification.class.getSimpleName() + " as sn where sn.start_date > :now and sn.start_date < :maxDate")
                .setParameter("now", new Date())
                .setParameter("maxDate", maxDate)
                .list();
    }

    @Override
    public SystemNotification createNotification(SystemNotificationSeverity severity, String title, String message, Date startDate, Date endDate) {
        Session session = sessionManager.getSession();
        SqlSystemNotification notification = new SqlSystemNotification();
        notification.setSeverity(severity);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setStartDate(startDate);
        notification.setEndDate(endDate);
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
    public void endNotification(SystemNotification notification) {
        Session session = sessionManager.getSession();
        notification.setEndDate(new Date());
        session.update(notification);
    }
}
