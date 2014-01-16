package com.altamiracorp.lumify.web.routes.graph;

import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.GraphUtil;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.type.GeoPoint;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Iterator;

public class GraphGeoLocationSearch extends BaseRequestHandler {
    private final Graph graph;

    @Inject
    public GraphGeoLocationSearch(final Graph graph) {
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final double latitude = getRequiredParameterAsDouble(request, "lat");
        final double longitude = getRequiredParameterAsDouble(request, "lon");
        final double radius = getRequiredParameterAsDouble(request, "radius");

        User user = getUser(request);
        Iterator<Vertex> vertexIterator = graph.query(user.getAuthorizations()).has(PropertyName.GEO_LOCATION.toString(), new GeoPoint(latitude, longitude, radius)).vertices().iterator();

        JSONObject results = new JSONObject();
        JSONArray vertices = new JSONArray();
        while (vertexIterator.hasNext()) {
            vertices.put(GraphUtil.toJson(vertexIterator.next()));
        }

        results.put("vertices", vertices);

        respondWithJson(response, results);
    }
}
