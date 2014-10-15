package io.lumify.core.model.systemNotification;

import io.lumify.core.security.LumifyVisibility;

import java.util.Date;
import java.util.List;

public abstract class SystemNotificationRepository {
    public static final String VISIBILITY_STRING = "systemNotification";
    public static final LumifyVisibility VISIBILITY = new LumifyVisibility(VISIBILITY_STRING);
    public static final String LUMIFY_SYSTEM_NOTIFICATION_CONCEPT_ID = "https//lumify.io/systemNotification";

    public abstract List<SystemNotification> getActiveNotifications();

    public abstract List<SystemNotification> getFutureNotifications(Date maxDate);

    public abstract SystemNotification createNotification(
            SystemNotificationSeverity severity,
            String title,
            String message,
            Date startDate,
            Date endDate
    );

    public abstract SystemNotification updateNotification(SystemNotification notification);

    public abstract void endNotification(SystemNotification notification);
}
