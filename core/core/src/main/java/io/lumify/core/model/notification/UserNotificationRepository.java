package io.lumify.core.model.notification;

import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.json.JSONObject;

import java.util.*;

public abstract class UserNotificationRepository extends NotificationRepository {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(UserNotificationRepository.class);
    private static final String LOCK_NAME = UserNotificationRepository.class.getName();
    private boolean shutdown;

    public abstract List<UserNotification> getActiveNotifications(User user);

    public abstract UserNotification createNotification(
            String userId,
            String title,
            String message,
            ExpirationAge expirationAge
    );

    public abstract void markRead(String[] rowKeys, User user);

    public abstract UserNotification getNotification(String rowKey, User user);

    public abstract UserNotification updateNotification(UserNotification notification);

    public abstract void endNotification(UserNotification notification);

    public static JSONObject toJSONObject(UserNotification notification) {
        JSONObject json = new JSONObject();
        json.put("id", notification.getId());
        json.put("type", "user");
        json.put("title", notification.getTitle());
        json.put("message", notification.getMessage());
        json.put("sentDate", notification.getSentDate().getTime());
        json.put("hash", hash(json.toString()));
        return json;
    }

    public static boolean isActive(UserNotification notification) {
        Date now = new Date();
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.setTime(notification.getSentDate());
        ExpirationAge age = notification.getExpirationAge();
        if (notification.isMarkedRead()) {
            return false;
        }
        if (age == null) {
            return true;
        }
        cal.add(age.getCalendarUnit(), age.getAmount());
        Date expirationDate = cal.getTime();
        return expirationDate.after(now);
    }

}
