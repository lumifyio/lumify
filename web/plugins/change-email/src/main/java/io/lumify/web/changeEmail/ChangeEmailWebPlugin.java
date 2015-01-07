package io.lumify.web.changeEmail;

import io.lumify.miniweb.Handler;
import io.lumify.miniweb.handlers.StaticResourceHandler;
import io.lumify.web.LumifyCsrfHandler;
import io.lumify.web.WebApp;
import io.lumify.web.WebAppPlugin;
import io.lumify.web.privilegeFilters.ReadPrivilegeFilter;

import javax.servlet.ServletContext;

public class ChangeEmailWebPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        Class<? extends Handler> authenticationHandlerClass = authenticationHandler.getClass();
        Class<? extends Handler> csrfHandlerClass = LumifyCsrfHandler.class;

        app.registerJavaScript("/io/lumify/web/changeEmail/changeEmail.js");
        app.registerResourceBundle("/io/lumify/web/changeEmail/messages.properties");

        app.get("/jsc/io/lumify/web/changeEmail/template.hbs", new StaticResourceHandler(this.getClass(), "/io/lumify/web/changeEmail/template.hbs", "text/plain"));

        app.post("/changeEmail", authenticationHandlerClass, csrfHandlerClass, ReadPrivilegeFilter.class, ChangeEmail.class);
    }
}
