package com.altamiracorp.lumify.web.routes.config;

import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Configuration extends BaseRequestHandler {
    private static final String EXPOSED_PROPERTIES_PREFIX = "web.ui.";

    @Inject
    public Configuration(
            final UserRepository userRepository,
            final com.altamiracorp.lumify.core.config.Configuration configuration) {
        super(userRepository, configuration);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {

        JSONObject results = new JSONObject();
        for (String key : getConfiguration().getKeys()) {
            if (key.startsWith(EXPOSED_PROPERTIES_PREFIX)) {
                results.put(key.replaceFirst(EXPOSED_PROPERTIES_PREFIX, ""), getConfiguration().get(key, ""));
            }
        }

        respondWithJson(response, results);
    }
}
