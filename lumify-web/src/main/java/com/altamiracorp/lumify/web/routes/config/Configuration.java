package com.altamiracorp.lumify.web.routes.config;

import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.query.Compare;
import com.altamiracorp.securegraph.query.GraphQuery;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

import static com.altamiracorp.lumify.core.util.GraphUtil.toJson;

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
