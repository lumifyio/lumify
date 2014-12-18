package io.lumify.web.routes.notification;

import com.google.inject.Inject;
import io.lumify.core.model.notification.UserNotificationRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UserNotificationMarkRead extends BaseRequestHandler {
    private final UserNotificationRepository userNotificationRepository;
    private static final String IDS_PARAMETER_NAME = "notificationIds[]";

    @Inject
    public UserNotificationMarkRead(
            final UserNotificationRepository userNotificationRepository,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final io.lumify.core.config.Configuration configuration
    ) {
        super(userRepository, workspaceRepository, configuration);
        this.userNotificationRepository = userNotificationRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String[] notificationIds = getRequiredParameterArray(request, IDS_PARAMETER_NAME);
        userNotificationRepository.markRead(notificationIds, getUser(request));
        respondWithSuccessJson(response);
    }
}
