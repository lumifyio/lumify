package io.lumify.web.auth.usernameonly;

import com.altamiracorp.miniweb.Handler;
import com.altamiracorp.miniweb.StaticFileHandler;
import com.altamiracorp.miniweb.StaticResourceHandler;
import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.web.AuthenticationHandler;
import io.lumify.web.WebApp;
import io.lumify.web.WebAppPlugin;
import io.lumify.web.auth.usernameonly.routes.Login;
import io.lumify.web.auth.usernameonly.routes.Logout;

import javax.servlet.ServletConfig;

public class UsernameOnlyWebAppPlugin implements WebAppPlugin {

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
    public void init(WebApp app, ServletConfig config, Class<? extends Handler> authenticator, AuthenticationHandler authenticationHandler) {

        // TODO
        // 1) Add route to replacement authentication.js component
        // app.get("/jsc/configuration/authentication/authentication.js", new StaticFileHandler(config, ??? component));

        // 2) Add routes for static files in plugin at /jsc/configuration/authentication/static/templates ?

        // 3) Add post route for logout

        StaticResourceHandler jsHandler = new StaticResourceHandler(this.getClass(), "/username-only/authentication.js", "application/javascript");
        StaticResourceHandler loginTemplateHandler = new StaticResourceHandler(this.getClass(), "/username-only/templates/login.hbs", "text/plain");

        app.get("/js/configuration/plugins/authentication/authentication.js", jsHandler);
        app.get("/jsc/configuration/plugins/authentication/authentication.js", jsHandler);
        app.get("/jsc/configuration/plugins/authentication/templates/login.hbs", jsHandler);
        app.post(AuthenticationHandler.LOGIN_PATH, new Login(this.userRepository, this.workspaceRepository, this.configuration));
        //app.get(AuthenticationHandler.LOGOUT_PATH, new Logout());
    }
}
