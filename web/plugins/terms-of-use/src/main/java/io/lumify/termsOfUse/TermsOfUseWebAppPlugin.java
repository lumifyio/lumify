package io.lumify.termsOfUse;

import io.lumify.miniweb.Handler;
import io.lumify.miniweb.handlers.StaticResourceHandler;
import io.lumify.web.WebApp;
import io.lumify.web.WebAppPlugin;

import javax.servlet.ServletContext;

public class TermsOfUseWebAppPlugin implements WebAppPlugin {
    public static final String TERMS_OF_USE_PATH = "/terms";

    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {

        app.get("/jsc/io/lumify/termsOfUse/terms-of-use.hbs",
                new StaticResourceHandler(getClass(), "/io/lumify/termsOfUse/terms-of-use.hbs", "text/html"));

        app.registerJavaScript("/io/lumify/termsOfUse/terms-of-use-plugin.js");
        app.registerResourceBundle("/io/lumify/termsOfUse/messages.properties");
        app.get(TERMS_OF_USE_PATH, TermsOfUse.class);
        app.post(TERMS_OF_USE_PATH, TermsOfUse.class);
    }
}
