package com.altamiracorp.lumify.web.routes.vertex;

import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Property;
import com.altamiracorp.securegraph.Text;
import com.altamiracorp.securegraph.property.StreamingPropertyValue;
import com.altamiracorp.securegraph.type.GeoPoint;
import com.google.inject.Inject;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
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
            if (property.getName().equals(PropertyName.GEO_LOCATION.toString())) {
                JSONObject geo = new JSONObject();
                GeoPoint geoPoint = (GeoPoint) property.getValue();
                geo.put("latitude", geoPoint.getLatitude());
                geo.put("longitude", geoPoint.getLongitude());
                resultsJson.put(property.getName(), geo);
            } else {
                Object value = property.getValue();
                if (value instanceof StreamingPropertyValue) {
                    continue;
                }
                if (value instanceof Date) {
                    value = ((Date) value).getTime();
                }
                if (value instanceof Text) {
                    value = ((Text) value).getText();
                }
                resultsJson.put(property.getName(), value);
            }
        }
        return resultsJson;
    }
}
