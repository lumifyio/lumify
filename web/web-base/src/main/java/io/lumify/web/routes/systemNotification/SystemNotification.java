package io.lumify.web.routes.systemNotification;

import com.google.inject.Inject;
import io.lumify.core.model.systemNotification.SystemNotificationRepository;
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

public class SystemNotification extends BaseRequestHandler {
    private static final String FUTURE_DAYS_PARAMETER_NAME = "futureDays";
    private static final int DEFAULT_FUTURE_DAYS = 10;
    private final SystemNotificationRepository systemNotificationRepository;

    @Inject
    public SystemNotification(
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
        notifications.put("active", new JSONArray(systemNotificationRepository.getActiveNotifications()));

        int futureDays = DEFAULT_FUTURE_DAYS;
        String futureDaysParameter = getOptionalParameter(request, FUTURE_DAYS_PARAMETER_NAME);
        if (futureDaysParameter != null) {
            futureDays = Integer.parseInt(futureDaysParameter);
        }
        Date maxDate = DateUtils.addDays(new Date(), futureDays);
        notifications.put("future", new JSONArray(systemNotificationRepository.getFutureNotifications(maxDate)));

        respondWithJson(response, notifications);
    }
}
