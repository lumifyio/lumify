package io.lumify.web.routes.map;

import io.lumify.core.config.Configuration;
import com.altamiracorp.miniweb.MustacheTemplateHandler;
import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;

public class MapInitHandler extends MustacheTemplateHandler {
    private final Configuration config;


    @Inject
    public MapInitHandler(final Configuration config) throws IOException {
        super();
        this.config = config;
    }

    @Override
    protected String getTemplateText() throws IOException {
        InputStream templateStream = Thread
                .currentThread()
                .getContextClassLoader()
                .getResourceAsStream("map-init.mustache");
        return IOUtils.toString(templateStream);
    }

    @Override
    protected Object getModel(HttpServletRequest request) {
        MapInitModel model = new MapInitModel();

        model.mapProvider = config.get(Configuration.MAP_PROVIDER);
        if (model.mapProvider == null) {
            model.mapProvider = "leaflet";
        }
        model.apiKey = config.get(Configuration.MAP_ACCESS_KEY);
        return model;
    }

    @Override
    protected String getContentType() {
        return "text/javascript";
    }

    private class MapInitModel {
        public String mapProvider;
        public String apiKey;
    }
}
