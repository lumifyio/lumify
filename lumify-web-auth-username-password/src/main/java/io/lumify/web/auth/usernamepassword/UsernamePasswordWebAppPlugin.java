package io.lumify.web.auth.usernamepassword;

import com.altamiracorp.miniweb.Handler;
import com.altamiracorp.miniweb.StaticResourceHandler;
import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.web.AuthenticationHandler;
import io.lumify.web.WebApp;
import io.lumify.web.WebAppPlugin;
import io.lumify.web.auth.usernamepassword.routes.Login;

import javax.servlet.ServletConfig;

public class UsernamePasswordWebAppPlugin implements WebAppPlugin {

    private UserRepository userRepository;
    private Configuration configuration;
    private WorkspaceRepository workspaceRepository;

    @Inject
    public void configure(UserRepository userRepository, WorkspaceRepository workspaceRepository, Configuration configuration) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.configuration = configuration;
    }

    @Override
    public void init(WebApp app, ServletConfig config, Handler authenticationHandler) {
        StaticResourceHandler jsHandler = new StaticResourceHandler(this.getClass(), "/username-only/authentication.js", "application/javascript");
        StaticResourceHandler loginTemplateHandler = new StaticResourceHandler(this.getClass(), "/username-only/templates/login.hbs", "text/plain");
        StaticResourceHandler lessHandler = new StaticResourceHandler(this.getClass(), "/username-only/less/login.less", "text/plain");

        app.get("/jsc/configuration/plugins/authentication/authentication.js", jsHandler);
        app.get("/jsc/configuration/plugins/authentication/templates/login.hbs", loginTemplateHandler);
        app.get("/jsc/configuration/plugins/authentication/less/login.less", lessHandler);

        app.post(AuthenticationHandler.LOGIN_PATH, new Login(this.userRepository, this.workspaceRepository, this.configuration));
    }
}
