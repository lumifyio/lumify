package io.lumify.core.model.systemNotification;

import io.lumify.core.security.LumifyVisibility;
import io.lumify.core.user.User;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;

public abstract class SystemNotificationRepository {
    public static final String VISIBILITY_STRING = "systemNotification";
    public static final LumifyVisibility VISIBILITY = new LumifyVisibility(VISIBILITY_STRING);

    public abstract List<SystemNotification> getActiveNotifications(User user);

    public abstract List<SystemNotification> getFutureNotifications(Date maxDate, User user);

    public abstract SystemNotification createNotification(
            SystemNotificationSeverity severity,
            String title,
            String message,
            Date startDate,
            Date endDate
    );

    public abstract SystemNotification updateNotification(SystemNotification notification);

    public abstract void endNotification(SystemNotification notification);

    public static JSONObject toJSONObject(SystemNotification notification) {
        JSONObject json = new JSONObject();
        json.put("id", notification.getId());
        json.put("severity", notification.getSeverity().toString());
        json.put("title", notification.getTitle());
        json.put("message", notification.getMessage());
        json.put("startDate", notification.getStartDate());
        json.put("endDate", notification.getEndDate());
        return json;
    }

    public static boolean isActive(SystemNotification notification) {
        Date now = new Date();
        Date endDate = notification.getEndDate();
        return notification.getStartDate().before(now) && (endDate == null || endDate.after(now));
    }
}
