package io.lumify.web.auth.usernamepassword.routes;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.ServletContextTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.miniweb.HandlerChain;
import io.lumify.miniweb.handlers.StaticResourceHandler;
import io.lumify.web.BaseRequestHandler;
import io.lumify.web.auth.usernamepassword.ForgotPasswordConfiguration;
import io.lumify.web.auth.usernamepassword.UsernamePasswordWebAppPlugin;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LookupToken extends BaseRequestHandler {
    public static final String TOKEN_PARAMETER_NAME = "token";
    private static final String TEMPLATE_PATH = "/username-password/templates";
    private static final String TEMPLATE_NAME = "changePasswordWithToken";
    private ForgotPasswordConfiguration forgotPasswordConfiguration;

    @Inject
    public LookupToken(UserRepository userRepository, WorkspaceRepository workspaceRepository, Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
        forgotPasswordConfiguration = new ForgotPasswordConfiguration();
        configuration.setConfigurables(forgotPasswordConfiguration, ForgotPasswordConfiguration.CONFIGURATION_PREFIX);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String token = getRequiredParameter(request, TOKEN_PARAMETER_NAME);
        User user = getUserRepository().findByPasswordResetToken(token);
        if (user != null) {
            Date now = new Date();
            if (user.getPasswordResetTokenExpirationDate().after(now)) {
                respondWithHtml(response, getHtml(getBaseUrl(request), token));
            } else {
                respondWithAccessDenied(response, "expired token");
            }
        } else {
            respondWithAccessDenied(response, "invalid token");
        }
    }

    private String getHtml(String baseUrl, String token) throws IOException {
        Map<String, String> context = new HashMap<String, String>();
        context.put("formAction", baseUrl + UsernamePasswordWebAppPlugin.CHANGE_PASSWORD_ROUTE);
        context.put("tokenParameterName", ChangePassword.TOKEN_PARAMETER_NAME);
        context.put("token", token);
        context.put("newPasswordLabel", forgotPasswordConfiguration.getNewPasswordLabel());
        context.put("newPasswordParameterName", ChangePassword.NEW_PASSWORD_PARAMETER_NAME);
        context.put("newPasswordConfirmationLabel", forgotPasswordConfiguration.getNewPasswordConfirmationLabel());
        context.put("newPasswordConfirmationParameterName", ChangePassword.NEW_PASSWORD_CONFIRMATION_PARAMETER_NAME);
        TemplateLoader templateLoader = new ClassPathTemplateLoader(TEMPLATE_PATH);
        Handlebars handlebars = new Handlebars(templateLoader);
        Template template = handlebars.compile(TEMPLATE_NAME);
        return template.apply(context);
    }
}
