package io.lumify.web.auth.usernamepassword.routes;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import io.lumify.web.auth.usernamepassword.ForgotPasswordConfiguration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

public class ChangePassword extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ChangePassword.class);
    public static final String TOKEN_PARAMETER_NAME = "token";
    public static final String NEW_PASSWORD_PARAMETER_NAME = "newPassword";
    public static final String NEW_PASSWORD_CONFIRMATION_PARAMETER_NAME = "newPasswordConfirmation";

    @Inject
    public ChangePassword(UserRepository userRepository, WorkspaceRepository workspaceRepository, Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String token = getRequiredParameter(request, TOKEN_PARAMETER_NAME);
        String newPassword = getRequiredParameter(request, NEW_PASSWORD_PARAMETER_NAME);
        String newPasswordConfirmation = getRequiredParameter(request, NEW_PASSWORD_CONFIRMATION_PARAMETER_NAME);

        User user = getUserRepository().findByPasswordResetToken(token);
        if (user != null) {
            Date now = new Date();
            if (user.getPasswordResetTokenExpirationDate().after(now)) {
                if (newPassword.length() > 0) {
                    if (newPassword.equals(newPasswordConfirmation)) {
                        getUserRepository().setPassword(user, newPassword);
                        getUserRepository().clearPasswordResetTokenAndExpirationDate(user);
                        LOGGER.info("changed password for user: %s", user.getUsername());
                    } else {
                        respondWithBadRequest(response, NEW_PASSWORD_CONFIRMATION_PARAMETER_NAME, "new password and new password confirmation do not match");
                    }
                } else {
                    respondWithBadRequest(response, NEW_PASSWORD_PARAMETER_NAME, "new password may not be blank");
                }
            } else {
                respondWithAccessDenied(response, "expired token");
            }
        } else {
            respondWithAccessDenied(response, "invalid token");
        }
    }
}
