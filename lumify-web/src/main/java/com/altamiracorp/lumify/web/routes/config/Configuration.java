package com.altamiracorp.lumify.web.routes.config;

import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

public class Configuration extends BaseRequestHandler {
    private static final String EXPOSED_PROPERTIES_PREFIX = "web.ui.";
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(Configuration.class);

    private final com.altamiracorp.lumify.core.config.Configuration configuration;

    @Inject
    public Configuration(final com.altamiracorp.lumify.core.config.Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {

        JSONObject results = new JSONObject();
        for (String key : configuration.getKeys()) {
            if (key.startsWith(EXPOSED_PROPERTIES_PREFIX)) {
                results.put(key.replaceFirst(EXPOSED_PROPERTIES_PREFIX, ""), configuration.get(key, ""));
            }
        }

        respondWithJson(response, results);
    }
}
