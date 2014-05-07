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
        StaticResourceHandler lessHandler = new StaticResourceHandler(this.getClass(), "/oauth/less/oauth.less", "text/plain");
        StaticResourceHandler loginTemplateHandler = new StaticResourceHandler(this.getClass(), "/oauth/templates/login.hbs", "text/plain");

        app.get("/jsc/configuration/plugins/authentication/authentication.js", jsHandler);
        app.get("/jsc/configuration/plugins/authentication/templates/login.hbs", loginTemplateHandler);
        app.get("/jsc/configuration/plugins/authentication/less/oauth.less", lessHandler);
        app.get("/jsc/configuration/plugins/authentication/img/twitter.png", new StaticResourceHandler(this.getClass(), "/oauth/img/twitter.png", "image/png"));
        app.get("/jsc/configuration/plugins/authentication/img/google.png", new StaticResourceHandler(this.getClass(), "/oauth/img/google.png", "image/png"));

        app.get("/oauth/twitter", new Twitter(this.twitterConfig, this.userRepository));


    }
}
