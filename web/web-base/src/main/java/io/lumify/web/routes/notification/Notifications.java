package io.lumify.web.routes.notification;

import com.google.inject.Inject;
import io.lumify.core.model.notification.SystemNotification;
import io.lumify.core.model.notification.SystemNotificationRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import org.apache.commons.lang.time.DateUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

public class Notifications extends BaseRequestHandler {
    private static final String FUTURE_DAYS_PARAMETER_NAME = "futureDays";
    private static final int DEFAULT_FUTURE_DAYS = 10;
    private final SystemNotificationRepository systemNotificationRepository;

    @Inject
    public Notifications(
            final SystemNotificationRepository systemNotificationRepository,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final io.lumify.core.config.Configuration configuration
    ) {
        super(userRepository, workspaceRepository, configuration);
        this.systemNotificationRepository = systemNotificationRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        JSONObject notifications = new JSONObject();

        JSONObject systemNotifications = new JSONObject();

        JSONArray activeNotifications = new JSONArray();
        for (SystemNotification notification : systemNotificationRepository.getActiveNotifications(getUser(request))) {
            activeNotifications.put(notification.toJSONObject());
        }
        systemNotifications.put("active", activeNotifications);

        int futureDays = DEFAULT_FUTURE_DAYS;
        String futureDaysParameter = getOptionalParameter(request, FUTURE_DAYS_PARAMETER_NAME);
        if (futureDaysParameter != null) {
            futureDays = Integer.parseInt(futureDaysParameter);
        }
        Date maxDate = DateUtils.addDays(new Date(), futureDays);
        JSONArray futureNotifications = new JSONArray();
        for (SystemNotification notification : systemNotificationRepository.getFutureNotifications(maxDate, getUser(request))) {
            futureNotifications.put(notification.toJSONObject());
        }
        systemNotifications.put("future", futureNotifications);

        JSONArray userNotifications = new JSONArray(); // TODO: return notifications for the current user (e.g. completed long running tasks)

        notifications.put("system", systemNotifications);
        notifications.put("user", userNotifications);
        respondWithJson(response, notifications);
    }
}
