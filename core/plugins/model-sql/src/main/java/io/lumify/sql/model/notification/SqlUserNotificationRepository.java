package io.lumify.sql.model.notification;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.lock.LockRepository;
import io.lumify.core.model.notification.*;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.sql.model.HibernateSessionManager;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Singleton
public class SqlUserNotificationRepository extends UserNotificationRepository {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(SqlUserNotificationRepository.class);
    private final HibernateSessionManager sessionManager;

    @Inject
    public SqlUserNotificationRepository(HibernateSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public UserNotification createNotification(String userId, String title, String message, ExpirationAge expirationAge) {
        Date now = new Date();
        String id = Long.toString(now.getTime()) + ":" + UUID.randomUUID().toString();
        Session session = sessionManager.getSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            SqlUserNotification notification = new SqlUserNotification();
            notification.setId(id);
            notification.setUserId(userId);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setSentDate(now);
            notification.setExpirationAge(expirationAge);
            notification.setMarkedRead(false);
            session.save(notification);
            transaction.commit();
            return notification;
        } catch (HibernateException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new LumifyException("HibernateException while creating", e);
        }
    }

    @Override
    public UserNotification getNotification(String notificationId, User user) {
        String userId = user.getUserId();
        Session session = sessionManager.getSession();
        List<UserNotification> notifications = session.createCriteria(SqlUserNotification.class)
                .add(Restrictions.eq("id", notificationId))
                .add(Restrictions.eq("userId", userId))
                .list();
        if (notificationId.length() != 1) {
            throw new LumifyException("failed to find a user notification with id = " + notificationId + " and user_id = " + userId);
        }
        return notifications.get(0);
    }

    @Override
    public void markRead(String[] notificationIds, User user) {
        for (String notificationId : notificationIds) {
            markRead(notificationId, user);
        }
    }

    private String dateFunction(Dialect dialect, ExpirationAgeUnit eau) {
        if (dialect.getClass() == org.hibernate.dialect.MySQL5InnoDBDialect.class) {
            return String.format("DATE_ADD(sent_date, INTERVAL expiration_age_amount %s)", eau.getMysqlInterval());
        } else if (dialect.getClass() == org.hibernate.dialect.H2Dialect.class) {
            return String.format("DATEADD('%s', expiration_age_amount, sent_date)", eau.getH2unit());
        } else {
            throw new LumifyException("unsupported Hibernate dialect: " + dialect.getClass().getName());
        }
    }

    private StringBuilder appendCaseStatement(StringBuilder sql, Dialect dialect) {
        sql.append(" CASE expiration_age_unit");
        for (ExpirationAgeUnit eau : ExpirationAgeUnit.values()) {
            sql.append(" WHEN '").append(eau.toString()).append("' THEN ").append(dateFunction(dialect, eau)).append(" >= :now");
        }
        return sql.append(" END");
    }

    @Override
    public List<UserNotification> getActiveNotifications(User user) {
        Session session = sessionManager.getSession();
        Dialect dialect = ((SessionFactoryImplementor) session.getSessionFactory()).getDialect();
        StringBuilder sql = new StringBuilder("SELECT * from user_notification WHERE user_id = :userId AND sent_date <= :now AND");
        sql = appendCaseStatement(sql, dialect);
        SQLQuery query = session.createSQLQuery(sql.toString());
        query.setString("userId", user.getUserId());
        query.setDate("now", new Date());
        List<UserNotification> activeNotifications = query.list();
        LOGGER.debug("returning %d active user notifications", activeNotifications.size());
        return activeNotifications;
    }

    public void markRead(String notificationId, User user) {
        Session session = sessionManager.getSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            String userId = user.getUserId();
            List<UserNotification> notifications = session.createCriteria(SqlUserNotification.class)
                    .add(Restrictions.eq("id", notificationId))
                    .add(Restrictions.eq("userId", userId))
                    .list();
            if (notificationId.length() != 1) {
                throw new LumifyException("failed to find a user notification with id = " + notificationId + " and user_id = " + userId);
            }
            UserNotification notification = notifications.get(0);
            notification.setMarkedRead(true);
            session.update(notification);
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new LumifyException("HibernateException while marking read", e);
        }
    }
}
