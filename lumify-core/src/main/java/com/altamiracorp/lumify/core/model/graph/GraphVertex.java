package com.altamiracorp.lumify.core.model.graph;

import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.model.ontology.VertexType;
import com.thinkaurelius.titan.core.attribute.Geoshape;
import com.tinkerpop.blueprints.Vertex;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class GraphVertex {
    private static final String SER_ID_PROPERTY = "id";
    private static final String SER_LATITUDE_PROPERTY = "latitude";
    private static final String SER_LONGITUDE_PROPERTY = "longitude";
    private static final String SER_PROPERTIES_PROPERTY = "properties";
    
    public abstract String getId();

    public abstract GraphVertex setProperty(String key, Object value);

    public abstract GraphVertex removeProperty(String key);

    public abstract Set<String> getPropertyKeys();

    public abstract Map<String, Object> getOldProperties ();

    public Object getProperty(PropertyName propertyKey) {
        return getProperty(propertyKey.toString());
    }

    public abstract Object getProperty(String propertyKey);

    public JSONObject toJson() {
        try {
            JSONObject json = new JSONObject();
            json.put(SER_ID_PROPERTY, getId());
            JSONObject propertiesJson = new JSONObject();
            for (String key : getPropertyKeys()) {
                if (key.equals(PropertyName.TYPE.toString())) {
                    propertiesJson.put(key, getProperty(key).toString().toLowerCase());
                } else if (key.equals(PropertyName.GEO_LOCATION.toString())) {
                    Double[] latlong = parseLatLong(getProperty(key));
                    propertiesJson.put(SER_LATITUDE_PROPERTY, latlong[0]);
                    propertiesJson.put(SER_LONGITUDE_PROPERTY, latlong[1]);
                } else {
                    propertiesJson.put(key, getProperty(key));
                }
            }
            json.put(SER_PROPERTIES_PROPERTY, propertiesJson);
            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T extends GraphVertex> T fromJson(final JSONObject json, final Class<T> clazz) throws InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        T vertex;
        String id = json.optString(SER_ID_PROPERTY, null);
        if (id != null) {
            id = id.trim();
        }
        // if the ID was persisted, try to create a new GraphVertex and set the ID
        if (id != null && !id.isEmpty()) {
            // try to find a constructor that will accept a single String argument and assume that
            // argument is the GraphVertex ID.
            try {
                Constructor<T> idCon = clazz.getConstructor(String.class);
                vertex = idCon.newInstance(id);
            } catch (NoSuchMethodException unused) {
                vertex = null;
            } catch (SecurityException unused) {
                vertex = null;
            }
            // no ID constructor found; create a new instance using the empty constructor
            // and attempt to find a setId(String) method.
            if (vertex == null) {
                vertex = clazz.newInstance();
                Method setId;
                try {
                    setId = clazz.getMethod("setId", String.class);
                } catch (NoSuchMethodException unused) {
                    setId = null;
                } catch (SecurityException unused) {
                    setId = null;
                }
                if (setId != null) {
                    setId.invoke(vertex, id);
                }
            }
        } else {
            // no ID persisted, create a new Vertex using the empty constructor
            vertex = clazz.newInstance();
        }
        // set the Vertex properties
        JSONObject props = json.optJSONObject(SER_PROPERTIES_PROPERTY);
        if (props != null) {
            Double latitude = null;
            Double longitude = null;
            for (String key : (Set<String>) props.keySet()) {
                if (SER_LATITUDE_PROPERTY.equals(key)) {
                    latitude = props.getDouble(key);
                } else if (SER_LONGITUDE_PROPERTY.equals(key)) {
                    longitude = props.getDouble(key);
                } else {
                    vertex.setProperty(key, props.get(key));
                }
            }
            if (latitude != null && longitude != null) {
                vertex.setProperty(PropertyName.GEO_LOCATION, Geoshape.point(latitude, longitude));
            }
        }
        return vertex;
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
        setProperty(PropertyName.TYPE, vertexType);
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
    
    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (!(other instanceof GraphVertex)) {
            return false;
        }
        GraphVertex otherVertex = (GraphVertex) other;
        
        String myId = getId();
        String theirId = otherVertex.getId();
        if (myId != theirId && (myId == null || !myId.equals(theirId))) {
            return false;
        }
        
        Set<String> myProps = getPropertyKeys();
        Set<String> theirProps = otherVertex.getPropertyKeys();
        if (myProps != theirProps && (myProps == null || !myProps.equals(theirProps))) {
            return false;
        }
        for (String key : myProps) {
            Object myVal = getProperty(key);
            Object theirVal = otherVertex.getProperty(key);
            if (myVal != theirVal && (myVal == null || !myVal.equals(theirVal))) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + (getId() != null ? getId().hashCode() : 0);
        for (String key : getPropertyKeys()) {
            Object val = getProperty(key);
            hash = 53 * hash + (val != null ? val.hashCode() : 0);
        }
        return hash;
    }
}
