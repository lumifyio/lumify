package com.altamiracorp.lumify.web.routes.vertex;

import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Property;
import com.google.inject.Inject;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Iterator;

public class VertexProperties extends BaseRequestHandler {
    private final Graph graph;

    @Inject
    public VertexProperties(final Graph graph) {
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String graphVertexId = getAttributeString(request, "graphVertexId");
        User user = getUser(request);

        Iterable<Property> properties = graph.getVertex(graphVertexId, user.getAuthorizations()).getProperties();
        JSONObject propertiesJson = propertiesToJson(properties);

        JSONObject json = new JSONObject();
        json.put("id", graphVertexId);
        json.put("properties", propertiesJson);

        respondWithJson(response, json);
    }

    public static JSONObject propertiesToJson(Iterable<Property> properties) throws JSONException {
        JSONObject resultsJson = new JSONObject();
        Iterator<Property> propertyIterator = properties.iterator();
        while (propertyIterator.hasNext()) {
            Property property = propertyIterator.next();
            resultsJson.put(property.getName(), property.getValue());
        }
        return resultsJson;
    }
}
