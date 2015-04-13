package io.lumify.web.changePassword;

import io.lumify.miniweb.Handler;
import io.lumify.miniweb.handlers.StaticResourceHandler;
import io.lumify.web.LumifyCsrfHandler;
import io.lumify.web.WebApp;
import io.lumify.web.WebAppPlugin;
import io.lumify.web.privilegeFilters.ReadPrivilegeFilter;

import javax.servlet.ServletContext;

public class ChangePasswordWebPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        Class<? extends Handler> authenticationHandlerClass = authenticationHandler.getClass();
        Class<? extends Handler> csrfHandlerClass = LumifyCsrfHandler.class;

        app.registerJavaScript("/io/lumify/web/changePassword/changePassword.js");
        app.registerCss("/io/lumify/web/changePassword/changePassword.css");
        app.registerResourceBundle("/io/lumify/web/changePassword/messages.properties");

        app.get("/jsc/io/lumify/web/changePassword/template.hbs", new StaticResourceHandler(ChangePasswordWebPlugin.class, "/io/lumify/web/changePassword/template.hbs", "text/plain"));

        app.post("/changePassword", authenticationHandlerClass, csrfHandlerClass, ReadPrivilegeFilter.class, ChangePassword.class);
    }
}
