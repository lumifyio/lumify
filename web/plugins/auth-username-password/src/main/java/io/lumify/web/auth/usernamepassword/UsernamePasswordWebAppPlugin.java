package io.lumify.web.auth.usernamepassword;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.miniweb.Handler;
import io.lumify.miniweb.handlers.StaticResourceHandler;
import io.lumify.core.bootstrap.InjectHelper;
import io.lumify.web.AuthenticationHandler;
import io.lumify.web.LumifyCsrfHandler;
import io.lumify.web.WebApp;
import io.lumify.web.WebAppPlugin;
import io.lumify.web.auth.usernamepassword.routes.Login;
import io.lumify.web.auth.usernamepassword.routes.ChangePassword;
import io.lumify.web.auth.usernamepassword.routes.LookupToken;
import io.lumify.web.auth.usernamepassword.routes.RequestToken;

import javax.servlet.ServletContext;

public class UsernamePasswordWebAppPlugin implements WebAppPlugin {
    public static final String LOOKUP_TOKEN_ROUTE = "/forgotPassword";
    private Configuration configuration;

    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        StaticResourceHandler jsHandler = new StaticResourceHandler(this.getClass(), "/username-password/authentication.js", "application/javascript");
        StaticResourceHandler loginTemplateHandler = new StaticResourceHandler(this.getClass(), "/username-password/templates/login.hbs", "text/plain");
        StaticResourceHandler lessHandler = new StaticResourceHandler(this.getClass(), "/username-password/less/login.less", "text/plain");

        app.get("/jsc/configuration/plugins/authentication/authentication.js", jsHandler);
        app.get("/jsc/configuration/plugins/authentication/templates/login.hbs", loginTemplateHandler);
        app.get("/jsc/configuration/plugins/authentication/less/login.less", lessHandler);

        app.post(AuthenticationHandler.LOGIN_PATH, InjectHelper.getInstance(Login.class));

        ForgotPasswordConfiguration forgotPasswordConfiguration = new ForgotPasswordConfiguration();
        configuration.setConfigurables(forgotPasswordConfiguration, ForgotPasswordConfiguration.CONFIGURATION_PREFIX);
        if (forgotPasswordConfiguration.isEnabled()) {
            app.post("/forgotPassword/requestToken", RequestToken.class);
            app.get(LOOKUP_TOKEN_ROUTE, LookupToken.class);
            app.post("/forgotPassword/changePassword", ChangePassword.class);
        }
    }

    @Inject
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
}
