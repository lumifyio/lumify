package com.altamiracorp.lumify.web.routes.vertex;

import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.model.graph.GraphRepository;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Property;
import com.google.inject.Inject;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class VertexProperties extends BaseRequestHandler {
    private final GraphRepository graphRepository;

    @Inject
    public VertexProperties(final GraphRepository repo) {
        graphRepository = repo;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String graphVertexId = getAttributeString(request, "graphVertexId");
        User user = getUser(request);

        Map<String, String> properties = graphRepository.getVertexProperties(graphVertexId, user);
        JSONObject propertiesJson = propertiesToJson(properties);

        JSONObject json = new JSONObject();
        json.put("id", graphVertexId);
        json.put("properties", propertiesJson);

        respondWithJson(response, json);
    }

    public static JSONObject propertiesToJson(Iterable<Property> properties) throws JSONException {
        JSONObject resultsJson = new JSONObject();
        for (Map.Entry<String, String> property : properties.entrySet()) {
            resultsJson.put(property.getKey(), property.getValue());
        }
        return resultsJson;
    }
}
