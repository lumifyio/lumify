package io.lumify.web.routes.notification;

import com.google.inject.Inject;
import io.lumify.core.model.notification.UserNotification;
import io.lumify.core.model.notification.UserNotificationRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class NotificationMarkRead extends BaseRequestHandler {
    private final UserNotificationRepository userNotificationRepository;
    private final WorkQueueRepository workQueueRepository;
    private static final String IDS_PARAMETER_NAME = "notificationIds[]";

    @Inject
    public NotificationMarkRead(
            final UserNotificationRepository userNotificationRepository,
            final WorkQueueRepository workQueueRepository,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final io.lumify.core.config.Configuration configuration
    ) {
        super(userRepository, workspaceRepository, configuration);
        this.userNotificationRepository = userNotificationRepository;
        this.workQueueRepository = workQueueRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String[] notificationIds = getRequiredParameterArray(request, IDS_PARAMETER_NAME);
        User user = getUser(request);

        userNotificationRepository.markRead(notificationIds, user);

        respondWithSuccessJson(response);
    }
}
