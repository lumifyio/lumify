package io.lumify.core.model.notification;

import io.lumify.core.user.User;
import org.json.JSONObject;

import java.util.*;

public abstract class UserNotificationRepository extends NotificationRepository {

    public abstract List<UserNotification> getActiveNotifications(User user);

    public abstract UserNotification createNotification(String user, String title, String message, String actionEvent, JSONObject actionPayload, ExpirationAge expirationAge);

    public abstract UserNotification getNotification(String notificationId, User user);

    public abstract void markRead(String[] notificationIds, User user);

    public UserNotification createNotification(String user, String title, String message, String actionUrl, ExpirationAge expirationAge) {
        JSONObject payload = new JSONObject();
        payload.put("url", actionUrl);
        return createNotification(user, title, message, Notification.ACTION_EVENT_EXTERNAL_URL, payload, expirationAge);
    }
    
    public UserNotification createNotification(String user, String title, String message, ExpirationAge expirationAge) {
        return createNotification(user, title, message, null, null, expirationAge);
    }

    public static JSONObject toJSONObject(UserNotification notification) {
        JSONObject json = new JSONObject();
        json.put("id", notification.getId());
        json.put("type", "user");
        json.put("title", notification.getTitle());
        json.put("message", notification.getMessage());
        json.put("sentDate", notification.getSentDate().getTime());
        if (notification.getActionEvent() != null) {
            JSONObject action = new JSONObject();
            action.put("event", notification.getActionEvent());
            action.putOpt("data", notification.getActionPayload());
            json.put("action", action);
        }
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
