package com.altamiracorp.lumify.core.model.ontology;

import com.altamiracorp.lumify.core.exception.LumifyException;
import com.altamiracorp.securegraph.Visibility;
import org.atteo.evo.inflector.English;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;

public abstract class Concept {
    private final String parentConceptIRI;
    private final Collection<OntologyProperty> properties;

    protected Concept(String parentConceptIRI, Collection<OntologyProperty> properties) {
        this.parentConceptIRI = parentConceptIRI;
        this.properties = properties;
    }

    public abstract String getTitle();

    public abstract boolean hasGlyphIconResource();

    public abstract String getColor();

    public abstract String getDisplayName();

    public abstract String getDisplayType();

    protected String getParentConceptIRI() {
        return this.parentConceptIRI;
    }

    public JSONObject toJson() {
        try {
            JSONObject result = new JSONObject();
            result.put("id", getTitle());
            result.put("title", getTitle());
            result.put("displayName", getDisplayName());
            if (getDisplayType() != null) {
                result.put("displayType", getDisplayType());
            }
            if (getParentConceptIRI() != null) {
                result.put("parentConcept", getParentConceptIRI());
            }
            if (getDisplayName() != null) {
                result.put("pluralDisplayName", English.plural(getDisplayName()));
            }
            if (hasGlyphIconResource()) {
                result.put("glyphIconHref", "resource?id=" + URLEncoder.encode(getTitle(), "utf8"));
            }
            if (getColor() != null) {
                result.put("color", getColor());
            }
            if (this.properties != null) {
                JSONArray propertiesJson = new JSONArray();
                for (OntologyProperty property : this.properties) {
                    propertiesJson.put(property.getTitle());
                }
                result.put("properties", propertiesJson);
            }
            return result;
        } catch (JSONException e) {
            throw new LumifyException("could not create json", e);
        } catch (UnsupportedEncodingException e) {
            throw new LumifyException("bad encoding", e);
        }
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", getDisplayName(), getTitle());
    }

    public static JSONArray toJsonConcepts(Iterable<Concept> concepts) {
        JSONArray conceptsJson = new JSONArray();
        for (Concept concept : concepts) {
            conceptsJson.put(concept.toJson());
        }
        return conceptsJson;
    }

    public abstract void setProperty(String name, Object value, Visibility visibility);

    public abstract InputStream getGlyphIcon();

    public abstract InputStream getMapGlyphIcon();
}
