package io.lumify.web.auth.usernameonly;

import com.altamiracorp.miniweb.Handler;
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
        Handler loginHandler = new Login(this.userRepository);
        app.get(AuthenticationHandler.LOGIN_PATH, loginHandler);
        app.post(AuthenticationHandler.LOGIN_PATH, loginHandler);
        app.get(AuthenticationHandler.LOGOUT_PATH, new Logout());
    }
}
