package com.altamiracorp.lumify.core.model.graph;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GraphRelationship {
    private final String id;
    private final String sourceVertexId;
    private final String destVertexId;
    private String label;
    private HashMap<String, Object> properties = new HashMap<String, Object>();

    public GraphRelationship(String id, String sourceVertexId, String destVertexId, String label) {
        this.id = id;
        this.sourceVertexId = sourceVertexId;
        this.destVertexId = destVertexId;
        this.label = label;
    }

    public String getSourceVertexId() {
        return sourceVertexId;
    }

    public String getDestVertexId() {
        return destVertexId;
    }

    public String getId() {
        return this.id;
    }

    public String getLabel() {
        return label;
    }

    public Set<String> getPropertyKeys() {
        return properties.keySet();
    }

    public Object getProperty(String propertyKey) {
        return properties.get(propertyKey);
    }

    public void setAllProperties(HashMap<String, Object> properties) {
        this.properties = properties;
    }

    public void setProperty(String key, String value) {
        properties.put(key, value);
    }

    public JSONObject toJson() {
        try {
            JSONObject json = new JSONObject();
            json.put("id", getId());
            json.put("label", getLabel());
            json.put("sourceVertexId", getSourceVertexId());
            json.put("destVertexId", getDestVertexId());

            JSONObject propertiesJson = new JSONObject();
            for (Map.Entry<String, Object> property : this.properties.entrySet()) {
                propertiesJson.put(property.getKey(), property.getValue().toString());
            }
            json.put("properties", propertiesJson);

            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
