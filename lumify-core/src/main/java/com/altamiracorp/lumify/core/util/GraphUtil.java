package com.altamiracorp.lumify.core.util;

import static com.altamiracorp.lumify.core.model.properties.EntityLumifyProperties.GEO_LOCATION;

import com.altamiracorp.securegraph.Direction;
import com.altamiracorp.securegraph.Edge;
import com.altamiracorp.securegraph.Element;
import com.altamiracorp.securegraph.Property;
import com.altamiracorp.securegraph.Text;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.type.GeoPoint;
import java.util.Collection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
            Object propertyValue = property.getValue();
            if (GEO_LOCATION.getKey().equals(property.getName())) {
                Double[] latlong = parseLatLong(propertyValue);
                propertiesJson.put("latitude", latlong[0]);
                propertiesJson.put("longitude", latlong[1]);
            } else {
                if (propertyValue instanceof Text) {
                    propertyValue = ((Text) propertyValue).getText();
                }
                propertiesJson.put(property.getName(), propertyValue);
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
