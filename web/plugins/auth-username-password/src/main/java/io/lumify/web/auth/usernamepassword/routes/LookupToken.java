package io.lumify.web.auth.usernamepassword.routes;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import io.lumify.web.auth.usernamepassword.ForgotPasswordConfiguration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

public class LookupToken extends BaseRequestHandler {
    public static final String TOKEN_PARAMETER_NAME = "token";

    @Inject
    public LookupToken(UserRepository userRepository, WorkspaceRepository workspaceRepository, Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String token = getRequiredParameter(request, TOKEN_PARAMETER_NAME);
        User user = getUserRepository().findByPasswordResetToken(token);
        if (user != null) {
            Date now = new Date();
            if (user.getPasswordResetTokenExpirationDate().after(now)) {
                // TODO: display change form including token
            } else {
                respondWithAccessDenied(response, "expired token");
            }
        } else {
            respondWithAccessDenied(response, "invalid token");
        }
    }
}
