package io.lumify.geocoder.bing;

import io.lumify.miniweb.Handler;
import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.web.WebApp;
import io.lumify.web.WebAppPlugin;

import javax.servlet.ServletContext;

public class BingGeocoderWebAppPlugin implements WebAppPlugin {
    private Configuration configuration;

    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        Class<? extends Handler> authenticationHandlerClass = authenticationHandler.getClass();

        if (BingGeocoder.verifyConfiguration(configuration)) {
            configuration.set(Configuration.WEB_GEOCODER_ENABLED, true);
            app.get("/map/geocode", authenticationHandlerClass, BingGeocoder.class);
        }
    }

    @Inject
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
}
