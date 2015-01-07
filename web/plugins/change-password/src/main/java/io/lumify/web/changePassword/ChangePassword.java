package io.lumify.web.changePassword;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ChangePassword extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ChangePassword.class);
    private static final String CURRENT_PASSWORD_PARAMETER_NAME = "currentPassword";
    private static final String NEW_PASSWORD_PARAMETER_NAME = "newPassword";
    private static final String NEW_PASSWORD_CONFIRMATION_PARAMETER_NAME = "newPasswordConfirmation";

    @Inject
    public ChangePassword(UserRepository userRepository,
                          WorkspaceRepository workspaceRepository,
                          Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        String currentPassword = getRequiredParameter(request, CURRENT_PASSWORD_PARAMETER_NAME);
        String newPassword = getRequiredParameter(request, NEW_PASSWORD_PARAMETER_NAME);
        String newPasswordConfirmation = getRequiredParameter(request, NEW_PASSWORD_CONFIRMATION_PARAMETER_NAME);

        if (user != null) {
            if (getUserRepository().isPasswordValid(user, currentPassword)) {
                if (newPassword.length() > 0) {
                    if (newPassword.equals(newPasswordConfirmation)) {
                        getUserRepository().setPassword(user, newPassword);
                        LOGGER.info("changed password for user: %s", user.getUsername());
                        respondWithSuccessJson(response);
                    } else {
                        respondWithBadRequest(response, NEW_PASSWORD_CONFIRMATION_PARAMETER_NAME, "new password and new password confirmation do not match");
                    }
                } else {
                    respondWithBadRequest(response, NEW_PASSWORD_PARAMETER_NAME, "new password may not be blank");
                }
            } else {
                LOGGER.warn("failed to change password for user: %s due to incorrect current password", user.getUsername());
                respondWithBadRequest(response, CURRENT_PASSWORD_PARAMETER_NAME, "incorrect current password");
            }
        } else {
            LOGGER.error("current user not found while attempting to change a password");
            respondWithAccessDenied(response, "current user not found");
        }
    }
}
