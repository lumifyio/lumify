package io.lumify.web.auth.usernameonly;

import com.altamiracorp.miniweb.Handler;
import com.altamiracorp.miniweb.StaticFileHandler;
import com.google.inject.Inject;
import io.lumify.core.model.user.UserRepository;
import io.lumify.web.AuthenticationHandler;
import io.lumify.web.WebApp;
import io.lumify.web.WebAppPlugin;
import io.lumify.web.auth.usernameonly.routes.Login;
import io.lumify.web.auth.usernameonly.routes.Logout;

import javax.servlet.ServletConfig;

public class UsernameOnlyWebAppPlugin implements WebAppPlugin {

    private UserRepository userRepository;

    @Inject
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void init(WebApp app, ServletConfig config, Class<? extends Handler> authenticator, AuthenticationHandler authenticationHandler) {

        // TODO
        // 1) Add route to replacement authentication.js component
        // app.get("/jsc/configuration/authentication/authentication.js", new StaticFileHandler(config, ??? component));

        // 2) Add routes for static files in plugin at /jsc/configuration/authentication/static/templates ?

        // 3) Add post route for logout

        app.post(AuthenticationHandler.LOGIN_PATH, new Login(this.userRepository));
        //app.get(AuthenticationHandler.LOGOUT_PATH, new Logout());
    }
}
