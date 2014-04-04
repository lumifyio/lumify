package com.altamiracorp.lumify.core.model.ontology;

import com.altamiracorp.securegraph.Vertex;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static com.altamiracorp.lumify.core.model.ontology.OntologyLumifyProperties.ONTOLOGY_TITLE;
import static com.altamiracorp.lumify.core.model.properties.LumifyProperties.DISPLAY_NAME;

public class Relationship {
    private final Vertex vertex;
    private final String sourceConceptIRI;
    private final String destConceptIRI;

    public Relationship(Vertex vertex, String sourceConceptIRI, String destConceptIRI) {
        this.vertex = vertex;
        this.sourceConceptIRI = sourceConceptIRI;
        this.destConceptIRI = destConceptIRI;
    }

    public String getIRI() {
        return ONTOLOGY_TITLE.getPropertyValue(vertex);
    }

    public String getDisplayName() {
        return DISPLAY_NAME.getPropertyValue(vertex);
    }

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
