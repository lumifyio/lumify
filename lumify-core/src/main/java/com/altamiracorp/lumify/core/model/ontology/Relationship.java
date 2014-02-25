package com.altamiracorp.lumify.core.model.ontology;

import com.altamiracorp.securegraph.Vertex;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static com.altamiracorp.lumify.core.model.ontology.OntologyLumifyProperties.ONTOLOGY_TITLE;
import static com.altamiracorp.lumify.core.model.properties.LumifyProperties.DISPLAY_NAME;

public class Relationship {
    private final Vertex vertex;
    private final String sourceConceptId;
    private final String destConceptId;

    public Relationship(Vertex vertex, String sourceConceptId, String destConceptId) {
        this.vertex = vertex;
        this.sourceConceptId = sourceConceptId;
        this.destConceptId = destConceptId;
    }

    public String getId() {
        return this.vertex.getId().toString();
    }

    public String getTitle() {
        return ONTOLOGY_TITLE.getPropertyValue(vertex);
    }

    public String getDisplayName() {
        return DISPLAY_NAME.getPropertyValue(vertex);
    }

    public String getSourceConceptId() {
        return sourceConceptId;
    }

    public String getDestConceptId() {
        return destConceptId;
    }

    public Vertex getVertex() {
        return vertex;
    }

    public JSONObject toJson() {
        try {
            JSONObject json = new JSONObject();
            json.put("id", getId());
            json.put("title", getTitle());
            json.put("displayName", getDisplayName());
            json.put("source", getSourceConceptId());
            json.put("dest", getDestConceptId());
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
