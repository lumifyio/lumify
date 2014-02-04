package com.altamiracorp.lumify.core.util;

import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.security.VisibilityTranslator;
import com.altamiracorp.securegraph.*;
import com.altamiracorp.securegraph.property.StreamingPropertyValue;
import com.altamiracorp.securegraph.type.GeoPoint;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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

    public static JSONObject toJson(Iterable<Property> properties) {
        JSONObject resultsJson = new JSONObject();
        for (Property property : properties) {
            if (property.getValue() instanceof StreamingPropertyValue) {
                continue;
            }
            resultsJson.put(property.getName(), GraphUtil.toJson(property));
        }
        return resultsJson;
    }

    public static JSONObject toJson(Property property) {
        JSONObject result = new JSONObject();

        Object value = property.getValue();
        if (value instanceof Text) {
            value = ((Text) value).getText();
        }

        if (value instanceof Date) {
            result.put("value", ((Date) value).getTime());
        } else if (value instanceof GeoPoint) {
            GeoPoint geoPoint = (GeoPoint) property.getValue();
            result.put("latitude", geoPoint.getLatitude());
            result.put("longitude", geoPoint.getLongitude());
        } else {
            result.put("value", value);
        }

        if (property.getVisibility() != null) {
            result.put(PropertyName.VISIBILITY.toString(), property.getVisibility().toString());
        }
        if (property.getMetadata() != null) {
            result.put(PropertyName.VISIBILITY_SOURCE.toString(), property.getMetadata().get(PropertyName.VISIBILITY_SOURCE.toString()));
        }

        return result;
    }

    public static ElementMutation setProperty(Element element, String propertyName, Object value, String visibilitySource, VisibilityTranslator visibilityTranslator) {
        Property oldProperty = element.getProperty(propertyName);
        Map<String, Object> propertyMetadata;
        if (oldProperty != null) {
            propertyMetadata = oldProperty.getMetadata();
        } else {
            propertyMetadata = new HashMap<String, Object>();
        }
        ElementMutation elementMutation = element.prepareMutation();

        Visibility visibility = visibilityTranslator.toVisibility(visibilitySource);
        propertyMetadata.put(PropertyName.VISIBILITY_SOURCE.toString(), visibilitySource);

        if (propertyName.equals(PropertyName.GEO_LOCATION.toString())) {
            GeoPoint geoPoint = (GeoPoint) value;
            elementMutation.setProperty(PropertyName.GEO_LOCATION.toString(), geoPoint, propertyMetadata, visibility);
            elementMutation.setProperty(PropertyName.GEO_LOCATION_DESCRIPTION.toString(), "", propertyMetadata, visibility);
        } else {
            elementMutation.setProperty(propertyName, value, propertyMetadata, visibility);
        }
        return elementMutation;
    }
}
