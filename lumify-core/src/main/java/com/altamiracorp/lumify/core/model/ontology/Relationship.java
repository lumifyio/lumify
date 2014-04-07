package com.altamiracorp.lumify.core.model.ontology;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class Relationship {
    private final String sourceConceptIRI;
    private final String destConceptIRI;

    protected Relationship(String sourceConceptIRI, String destConceptIRI) {
        this.sourceConceptIRI = sourceConceptIRI;
        this.destConceptIRI = destConceptIRI;
    }

    public abstract String getIRI();

    public abstract String getDisplayName();

    public String getSourceConceptIRI() {
        return sourceConceptIRI;
    }

    public String getDestConceptIRI() {
        return destConceptIRI;
    }

    public JSONObject toJson() {
        try {
            JSONObject json = new JSONObject();
            json.put("title", getIRI());
            json.put("displayName", getDisplayName());
            json.put("source", getSourceConceptIRI());
            json.put("dest", getDestConceptIRI());
            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static JSONArray toJsonRelationships(Iterable<Relationship> relationships) {
        JSONArray results = new JSONArray();
        for (Relationship vertex : relationships) {
            results.put(vertex.toJson());
        }
        return results;
    }
}
