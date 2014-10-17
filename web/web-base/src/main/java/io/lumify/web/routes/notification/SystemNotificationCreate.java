package io.lumify.web.routes.notification;

import com.google.inject.Inject;
import io.lumify.core.model.systemNotification.SystemNotificationRepository;
import io.lumify.core.model.systemNotification.SystemNotificationSeverity;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SystemNotificationCreate extends BaseRequestHandler {
    private final SystemNotificationRepository systemNotificationRepository;
    private static final String SEVERITY_PARAMETER_NAME = "severity";
    private static final String TITLE_PARAMETER_NAME = "title";
    private static final String MESSAGE_PARAMETER_NAME = "message";
    private static final String START_DATE_PARAMETER_NAME = "startDate";
    private static final String END_DATE_PARAMETER_NAME = "endDate";
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm";

    @Inject
    public SystemNotificationCreate(
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
        String severityParameter = getRequiredParameter(request, SEVERITY_PARAMETER_NAME);
        SystemNotificationSeverity severity = SystemNotificationSeverity.valueOf(severityParameter);
        String title = getRequiredParameter(request, TITLE_PARAMETER_NAME);
        String message = getRequiredParameter(request, MESSAGE_PARAMETER_NAME);
        String startDateParameter = getRequiredParameter(request, START_DATE_PARAMETER_NAME);
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        Date startDate = sdf.parse(startDateParameter);
        String endDateParameter = getOptionalParameter(request, END_DATE_PARAMETER_NAME);
        Date endDate = endDateParameter != null ? sdf.parse(endDateParameter) : null;

        systemNotificationRepository.createNotification(severity, title, message, startDate, endDate);

        respondWithSuccessJson(response);
    }
}
