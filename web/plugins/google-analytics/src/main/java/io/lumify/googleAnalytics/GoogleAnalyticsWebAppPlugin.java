package io.lumify.googleAnalytics;

import io.lumify.miniweb.Handler;
import io.lumify.web.WebApp;
import io.lumify.web.WebAppPlugin;

import javax.servlet.ServletContext;

public class GoogleAnalyticsWebAppPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        app.registerJavaScript("/io/lumify/googleAnalytics/google-analytics-plugin.js");
    }
}
