package com.altamiracorp.lumify.core.model.ontology;

import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public abstract class Relationship extends GraphVertex {
    public abstract String getId();

    public abstract String getTitle();

    public abstract String getDisplayName();

    public abstract Concept getSourceConcept();

    public abstract Concept getDestConcept();

    public JSONObject toJson() {
        try {
            JSONObject json = new JSONObject();
            json.put("id", getId());
            json.put("title", getTitle());
            json.put("displayName", getDisplayName());
            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static JSONArray toJsonRelationships(List<Relationship> relationships) {
        JSONArray results = new JSONArray();
        for (GraphVertex vertex : relationships) {
            results.put(vertex.toJson());
        }
        return results;
    }
}
