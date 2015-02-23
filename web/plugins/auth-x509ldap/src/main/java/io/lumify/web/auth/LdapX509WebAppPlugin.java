package io.lumify.web.auth;

import io.lumify.miniweb.Handler;
import io.lumify.miniweb.handlers.StaticResourceHandler;
import io.lumify.core.bootstrap.InjectHelper;
import io.lumify.web.AuthenticationHandler;
import io.lumify.web.WebApp;
import io.lumify.web.WebAppPlugin;

import javax.servlet.ServletContext;

public class LdapX509WebAppPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        StaticResourceHandler jsHandler = new StaticResourceHandler(this.getClass(), "/ldap-x509/authentication.js", "application/javascript");
        StaticResourceHandler loginTemplateHandler = new StaticResourceHandler(this.getClass(), "/ldap-x509/templates/login.hbs", "text/plain");
        StaticResourceHandler lessHandler = new StaticResourceHandler(this.getClass(), "/ldap-x509/less/login.less", "text/plain");

        app.registerJavaScript("/ldap-x509/logout.js");

        app.get("/logout.html", new StaticResourceHandler(this.getClass(), "/ldap-x509/logout.html", "text/html"));
        app.get("/jsc/configuration/plugins/authentication/css/logout.css", new StaticResourceHandler(this.getClass(), "/ldap-x509/css/logout.css", "text/css"));
        app.get("/jsc/configuration/plugins/authentication/authentication.js", jsHandler);
        app.get("/jsc/configuration/plugins/authentication/templates/login.hbs", loginTemplateHandler);
        app.get("/jsc/configuration/plugins/authentication/less/login.less", lessHandler);

        app.post(AuthenticationHandler.LOGIN_PATH, InjectHelper.getInstance(LdapX509AuthenticationHandler.class));
    }
}
