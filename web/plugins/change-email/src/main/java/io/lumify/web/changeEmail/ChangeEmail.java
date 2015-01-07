package io.lumify.web.changeEmail;

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

public class ChangeEmail extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ChangeEmail.class);
    private static final String EMAIL_PARAMETER_NAME = "email";

    @Inject
    public ChangeEmail(UserRepository userRepository,
                       WorkspaceRepository workspaceRepository,
                       Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        String email = getRequiredParameter(request, EMAIL_PARAMETER_NAME);

        if (user != null) {
            if (email.length() > 0) {
                    getUserRepository().setEmailAddress(user, email);
                    LOGGER.info("changed email for user: %s", user.getUsername());
                    respondWithSuccessJson(response);
            } else {
                respondWithBadRequest(response, EMAIL_PARAMETER_NAME, "new email may not be blank");
            }
        } else {
            LOGGER.error("current user not found while attempting to change email");
            respondWithAccessDenied(response, "current user not found");
        }
    }
}
