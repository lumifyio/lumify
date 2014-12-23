package io.lumify.core.model.notification;

import io.lumify.core.user.User;
import org.json.JSONObject;

import java.util.*;

public abstract class UserNotificationRepository extends NotificationRepository {

    public abstract List<UserNotification> getActiveNotifications(User user);

    public abstract UserNotification createNotification(
            String userId,
            String title,
            String message,
            ExpirationAge expirationAge
    );

    public abstract UserNotification getNotification(String notificationId, User user);

    public abstract void markRead(String[] notificationIds, User user);

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
        if (notification.isMarkedRead()) {
            return false;
        }
        Date now = new Date();
        Date expirationDate = getExpirationDate(notification);
        Date sentDate = notification.getSentDate();
        if (expirationDate.equals(now) || sentDate.equals(now)) {
            return true;
        }
        return notification.getSentDate().before(now) && getExpirationDate(notification).after(now);
    }

    public static Date getExpirationDate(UserNotification notification) {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.setTime(notification.getSentDate());
        ExpirationAge age = notification.getExpirationAge();
        cal.add(age.getExpirationAgeUnit().getCalendarUnit(), age.getAmount());
        return cal.getTime();
    }
}
