package io.lumify.web.auth.oauth;

import io.lumify.miniweb.Handler;
import io.lumify.miniweb.handlers.StaticResourceHandler;
import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.web.WebApp;
import io.lumify.web.WebAppPlugin;
import io.lumify.web.auth.oauth.routes.Google;
import io.lumify.web.auth.oauth.routes.Twitter;

import javax.servlet.ServletContext;

public class OAuthWebAppPlugin implements WebAppPlugin {
    private OAuthConfiguration twitterConfig;
    private OAuthConfiguration googleConfig;
    private UserRepository userRepository;

    @Inject
    public void configure(Configuration config, UserRepository userRepository) {
        this.userRepository = userRepository;
        this.twitterConfig = new OAuthConfiguration();
        config.setConfigurables(this.twitterConfig, "oauth.twitter");
        this.googleConfig = new OAuthConfiguration();
        config.setConfigurables(this.googleConfig, "oauth.google");
    }

    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        StaticResourceHandler jsHandler = new StaticResourceHandler(this.getClass(), "/oauth/authentication.js", "application/javascript");
        StaticResourceHandler lessHandler = new StaticResourceHandler(this.getClass(), "/oauth/less/oauth.less", "text/plain");
        StaticResourceHandler loginTemplateHandler = new StaticResourceHandler(this.getClass(), "/oauth/templates/login.hbs", "text/plain");

        app.get("/jsc/configuration/plugins/authentication/authentication.js", jsHandler);
        app.get("/jsc/configuration/plugins/authentication/templates/login.hbs", loginTemplateHandler);
        app.get("/jsc/configuration/plugins/authentication/less/oauth.less", lessHandler);
        app.get("/jsc/configuration/plugins/authentication/img/twitter.png", new StaticResourceHandler(this.getClass(), "/oauth/img/twitter.png", "image/png"));
        app.get("/jsc/configuration/plugins/authentication/img/google.png", new StaticResourceHandler(this.getClass(), "/oauth/img/google.png", "image/png"));

        app.get("/oauth/twitter", new Twitter(this.twitterConfig, this.userRepository));
        app.get("/oauth/google", new Google(this.googleConfig, this.userRepository));
    }
}
