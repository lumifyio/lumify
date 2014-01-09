package com.altamiracorp.lumify.core.model.ontology;

import com.altamiracorp.securegraph.Vertex;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import static com.altamiracorp.lumify.core.util.ObjectHelper.toStringOrNull;

public class Relationship {
    private final Vertex vertex;
    private final Concept sourceConcept;
    private final Concept destConcept;

    public Relationship(Vertex vertex, Concept sourceConcept, Concept destConcept) {
        this.vertex = vertex;
        this.sourceConcept = sourceConcept;
        this.destConcept = destConcept;
    }

    public Object getId() {
        return this.vertex.getId();
    }

    public String getTitle() {
        return toStringOrNull(this.vertex.getPropertyValue(PropertyName.ONTOLOGY_TITLE.toString(), 0));
    }

    public String getDisplayName() {
        return toStringOrNull(this.vertex.getPropertyValue(PropertyName.DISPLAY_NAME.toString(), 0));
    }

    public Concept getSourceConcept() {
        return this.sourceConcept;
    }

    public Concept getDestConcept() {
        return this.destConcept;
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
            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static JSONArray toJsonRelationships(List<Relationship> relationships) {
        JSONArray results = new JSONArray();
        for (Relationship vertex : relationships) {
            results.put(vertex.toJson());
        }
        return results;
    }
}
