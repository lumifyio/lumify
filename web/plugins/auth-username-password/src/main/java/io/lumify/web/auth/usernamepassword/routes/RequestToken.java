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
import io.lumify.web.auth.usernamepassword.UsernamePasswordWebAppPlugin;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class RequestToken extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(RequestToken.class);
    private static final String USERNAME_PARAMETER_NAME = "username";
    private ForgotPasswordConfiguration forgotPasswordConfiguration;

    @Inject
    public RequestToken(UserRepository userRepository, WorkspaceRepository workspaceRepository, Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
        forgotPasswordConfiguration = new ForgotPasswordConfiguration();
        configuration.setConfigurables(forgotPasswordConfiguration, ForgotPasswordConfiguration.CONFIGURATION_PREFIX);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String username = getOptionalParameter(request, USERNAME_PARAMETER_NAME);

        if (username != null) {
            User user = getUserRepository().findByUsername(username);
            if (user != null) {
                if (user.getEmailAddress() != null) {
                    String baseUrl = getBaseUrl(request);
                    createTokenAndSendEmail(baseUrl, user);
                    respondWithSuccessJson(response);
                } else {
                    respondWithBadRequest(response, USERNAME_PARAMETER_NAME, "no e-mail address available for user");
                }
            } else {
                respondWithBadRequest(response, USERNAME_PARAMETER_NAME, "username not found");
            }
        } else {
            respondWithBadRequest(response, USERNAME_PARAMETER_NAME, "username required");
        }
    }

    private void createTokenAndSendEmail(String baseUrl, User user) {
        String token = new BigInteger(120, new SecureRandom()).toString(32);;
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.add(Calendar.MINUTE, forgotPasswordConfiguration.getTokenLifetimeMinutes());
        getUserRepository().setPasswordResetTokenAndExpirationDate(user, token, cal.getTime());

        String url = baseUrl + UsernamePasswordWebAppPlugin.LOOKUP_TOKEN_ROUTE + "?" + LookupToken.TOKEN_PARAMETER_NAME + "=" + token;
        LOGGER.debug(url);

        // TODO: send email
    }
}
