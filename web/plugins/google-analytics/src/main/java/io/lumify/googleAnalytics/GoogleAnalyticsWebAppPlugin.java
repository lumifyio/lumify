package io.lumify.googleAnalytics;

import com.altamiracorp.miniweb.Handler;
import io.lumify.web.WebApp;
import io.lumify.web.WebAppPlugin;

import javax.servlet.ServletConfig;

public class GoogleAnalyticsWebAppPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletConfig config, Handler authenticationHandler) {
        app.registerJavaScript("/io/lumify/googleAnalytics/google-analytics-plugin.js");
    }
}
