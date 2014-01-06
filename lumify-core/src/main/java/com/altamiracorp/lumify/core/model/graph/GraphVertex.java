package com.altamiracorp.lumify.core.model.graph;

import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.model.ontology.VertexType;
import com.thinkaurelius.titan.core.attribute.Geoshape;
import com.tinkerpop.blueprints.Vertex;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

public abstract class GraphVertex {
    public abstract String getId();

    public abstract GraphVertex setProperty(String key, Object value);

    public abstract GraphVertex removeProperty(String key);

    public abstract Set<String> getPropertyKeys();

    public abstract HashMap<String, Object> getOldProperties ();

    public Object getProperty(PropertyName propertyKey) {
        return getProperty(propertyKey.toString());
    }

    public abstract Object getProperty(String propertyKey);

    public JSONObject toJson() {
        try {
            JSONObject json = new JSONObject();
            json.put("id", getId());
            JSONObject propertiesJson = new JSONObject();
            for (String key : getPropertyKeys()) {
                if (key.equals("_type")) {
                    propertiesJson.put(key, getProperty(key).toString().toLowerCase());
                } else if (key.equals("geoLocation")) {
                    Double[] latlong = parseLatLong(getProperty(key));
                    propertiesJson.put("latitude", latlong[0]);
                    propertiesJson.put("longitude", latlong[1]);
                } else {
                    propertiesJson.put(key, getProperty(key));
                }
            }
            json.put("properties", propertiesJson);
            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static JSONArray toJson(List<GraphVertex> vertices) {
        JSONArray results = new JSONArray();
        for (GraphVertex vertex : vertices) {
            results.put(vertex.toJson());
        }
        return results;
    }

    public static JSONArray toJsonPath(List<List<GraphVertex>> paths) {
        JSONArray results = new JSONArray();
        for (List<GraphVertex> path : paths) {
            results.put(toJson(path));
        }
        return results;
    }

    public void update(GraphVertex newGraphVertex) {
        for (String propertyKey : newGraphVertex.getPropertyKeys()) {
            setProperty(propertyKey, newGraphVertex.getProperty(propertyKey));
        }
    }

    public void setProperty(PropertyName propertyName, Object value) {
        setProperty(propertyName.toString(), value);
    }

    public void setType(VertexType vertexType) {
        setType(vertexType.toString());
    }

    public void setType(String vertexType) {
        setProperty(PropertyName.CONCEPT_TYPE, vertexType);
    }

    public Vertex getVertex() {
        return null;
    }

    public static Double[] parseLatLong(Object val) {
        Double[] result = new Double[2];
        if (val instanceof String) {
            String valStr = (String) val;
            String[] latlong = valStr.substring(valStr.indexOf('[') + 1, valStr.indexOf(']')).split(",");
            result[0] = Double.parseDouble(latlong[0]);
            result[1] = Double.parseDouble(latlong[1]);
        } else if (val instanceof Geoshape) {
            Geoshape valGeoShape = (Geoshape) val;
            result[0] = (double) valGeoShape.getPoint().getLatitude();
            result[1] = (double) valGeoShape.getPoint().getLongitude();
        }
        return result;
    }
}
