package io.lumify.web.routes.notification;

import com.google.inject.Inject;
import io.lumify.core.model.systemNotification.SystemNotification;
import io.lumify.core.model.systemNotification.SystemNotificationRepository;
import io.lumify.core.model.systemNotification.SystemNotificationSeverity;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SystemNotificationDelete extends BaseRequestHandler {
    private final SystemNotificationRepository systemNotificationRepository;
    private final WorkQueueRepository workQueueRepository;
    private static final String ID_PARAMETER_NAME = "notificationId";

    @Inject
    public SystemNotificationDelete(
            final SystemNotificationRepository systemNotificationRepository,
            final WorkQueueRepository workQueueRepository,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final io.lumify.core.config.Configuration configuration
    ) {
        super(userRepository, workspaceRepository, configuration);
        this.systemNotificationRepository = systemNotificationRepository;
        this.workQueueRepository = workQueueRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String notificationId = getRequiredParameter(request, ID_PARAMETER_NAME);

        SystemNotification notification = systemNotificationRepository.getNotification(notificationId, getUser(request));
        if (notification == null) {
            respondWithNotFound(response);
            return;
        }

        systemNotificationRepository.endNotification(notification);
        workQueueRepository.pushSystemNotificationEnded(notificationId);

        respondWithSuccessJson(response);
    }
}
