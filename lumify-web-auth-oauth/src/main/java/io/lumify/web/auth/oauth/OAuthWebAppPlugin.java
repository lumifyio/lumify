package io.lumify.web.auth.oauth;

import com.altamiracorp.miniweb.Handler;
import com.altamiracorp.miniweb.StaticResourceHandler;
import io.lumify.web.AuthenticationProvider;
import io.lumify.web.WebApp;
import io.lumify.web.WebAppPlugin;
import io.lumify.web.auth.oauth.routes.Login;
import io.lumify.web.auth.oauth.routes.Twitter;

import javax.servlet.ServletConfig;

public class OAuthWebAppPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletConfig config, Class<? extends Handler> authenticator, AuthenticationProvider authenticatorInstance) {
        app.get("/oauth/login", new Login());

        app.get("/oauth/twitter", new Twitter());

        app.get("/oauth/static/img/sign-in-with-twitter-link.png", new StaticResourceHandler(this.getClass(), "/oauth/static/img/sign-in-with-twitter-link.png", "image/png"));
        app.get("/oauth/success.html", new StaticResourceHandler(this.getClass(), "/oauth/static/success.html", "text/html"));
    }
}
