package io.lumify.web.auth.oauth;

import com.altamiracorp.miniweb.Handler;
import com.altamiracorp.miniweb.StaticResourceHandler;
import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.web.WebApp;
import io.lumify.web.WebAppPlugin;
import io.lumify.web.auth.oauth.routes.Twitter;

import javax.servlet.ServletConfig;

public class OAuthWebAppPlugin implements WebAppPlugin {
    private TwitterOAuthConfiguration twitterConfig;
    private UserRepository userRepository;

    @Inject
    public void configure(Configuration config, UserRepository userRepository) {
        this.userRepository = userRepository;
        this.twitterConfig = new TwitterOAuthConfiguration();
        config.setConfigurables(this.twitterConfig, "oauth.twitter");
    }

    @Override
    public void init(WebApp app, ServletConfig config, Handler authenticationHandler) {
        StaticResourceHandler jsHandler = new StaticResourceHandler(this.getClass(), "/oauth/authentication.js", "application/javascript");
        StaticResourceHandler loginTemplateHandler = new StaticResourceHandler(this.getClass(), "/oauth/templates/login.hbs", "text/plain");

        app.get("/js/configuration/plugins/authentication/authentication.js", jsHandler);
        app.get("/jsc/configuration/plugins/authentication/authentication.js", jsHandler);
        app.get("/jsc/configuration/plugins/authentication/templates/login.hbs", loginTemplateHandler);

        app.get("/oauth/twitter", new Twitter(this.twitterConfig, this.userRepository));

        app.get("/oauth/img/sign-in-with-twitter-link.png", new StaticResourceHandler(this.getClass(), "/oauth/img/sign-in-with-twitter-link.png", "image/png"));
    }
}
