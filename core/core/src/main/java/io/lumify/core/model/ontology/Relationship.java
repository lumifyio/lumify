package io.lumify.core.model.ontology;

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

    public abstract Iterable<String> getInverseOfIRIs();

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

            Iterable<String> inverseOfIRIs = getInverseOfIRIs();
            JSONArray inverseOfsJson = new JSONArray();
            for (String inverseOfIRI : inverseOfIRIs) {
                JSONObject inverseOfJson = new JSONObject();
                inverseOfJson.put("iri", inverseOfIRI);
                inverseOfJson.put("primaryIri", getPrimaryInverseOfIRI(getIRI(), inverseOfIRI));
                inverseOfsJson.put(inverseOfJson);
            }
            if (inverseOfsJson.length() > 0) {
                json.put("inverseOfs", inverseOfsJson);
            }

            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getPrimaryInverseOfIRI(String iri1, String iri2) {
        if (iri1.compareTo(iri2) > 0) {
            return iri2;
        }
        return iri1;
    }

    public static JSONArray toJsonRelationships(Iterable<Relationship> relationships) {
        JSONArray results = new JSONArray();
        for (Relationship vertex : relationships) {
            results.put(vertex.toJson());
        }
        return results;
    }
}
