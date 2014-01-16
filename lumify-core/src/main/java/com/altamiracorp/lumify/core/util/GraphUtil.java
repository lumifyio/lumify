package com.altamiracorp.lumify.core.util;

import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.securegraph.*;
import com.altamiracorp.securegraph.type.GeoPoint;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;

public class GraphUtil {
    public static JSONArray toJson(Collection<Element> elements) {
        JSONArray result = new JSONArray();
        for (Element element : elements) {
            result.put(toJson(element));
        }
        return result;
    }

    public static JSONObject toJson(Element element) {
        if (element instanceof Vertex) {
            return toJson((Vertex) element);
        }
        if (element instanceof Edge) {
            return toJson((Edge) element);
        }
        throw new RuntimeException("Unexpected element type: " + element.getClass().getName());
    }

    public static JSONObject toJson(Vertex vertex) {
        try {
            JSONObject json = new JSONObject();
            json.put("id", vertex.getId());
            json.put("properties", toJson(vertex.getProperties()));
            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static JSONObject toJson(Edge edge) {
        try {
            JSONObject json = new JSONObject();
            json.put("id", edge.getId());
            json.put("label", edge.getLabel());
            json.put("sourceVertexId", edge.getVertexId(Direction.OUT));
            json.put("destVertexId", edge.getVertexId(Direction.IN));
            json.put("properties", toJson(edge.getProperties()));
            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private static JSONObject toJson(Iterable<Property> properties) {
        // TODO handle multi-valued properties
        JSONObject propertiesJson = new JSONObject();
        for (Property property : properties) {
            if (property.getName().equals(PropertyName.GEO_LOCATION.toString())) {
                Double[] latlong = parseLatLong(property.getValue());
                propertiesJson.put("latitude", latlong[0]);
                propertiesJson.put("longitude", latlong[1]);
            } else {
                propertiesJson.put(property.getName(), property.getValue().toString());
            }
        }
        return propertiesJson;
    }

    public static Double[] parseLatLong(Object val) {
        Double[] result = new Double[2];
        if (val instanceof String) {
            String valStr = (String) val;
            String[] latlong = valStr.substring(valStr.indexOf('[') + 1, valStr.indexOf(']')).split(",");
            result[0] = Double.parseDouble(latlong[0]);
            result[1] = Double.parseDouble(latlong[1]);
        } else if (val instanceof GeoPoint) {
            GeoPoint valGeoPoint = (GeoPoint) val;
            result[0] = valGeoPoint.getLatitude();
            result[1] = valGeoPoint.getLongitude();
        }
        return result;
    }
}
