package com.altamiracorp.lumify.core.model.ontology;

import com.altamiracorp.lumify.core.exception.LumifyException;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.Visibility;
import org.atteo.evo.inflector.English;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;

import static com.altamiracorp.lumify.core.model.ontology.OntologyLumifyProperties.*;
import static com.altamiracorp.lumify.core.model.properties.LumifyProperties.DISPLAY_NAME;
import static com.altamiracorp.lumify.core.model.properties.LumifyProperties.GLYPH_ICON;

public class Concept {

    private final Vertex vertex;
    private final Collection<OntologyProperty> properties;
    private final Vertex parentConceptVertex;

    public Concept(Vertex vertex) {
        this(vertex, null, null);
    }

    public Concept(Vertex vertex, Vertex parentConceptVertex, Collection<OntologyProperty> properties) {
        this.vertex = vertex;
        this.parentConceptVertex = parentConceptVertex;
        this.properties = properties;
    }

    public String getId() {
        return vertex.getId().toString();
    }

    public String getTitle() {
        return ONTOLOGY_TITLE.getPropertyValue(vertex);
    }

    public boolean hasGlyphIconResource() {
        // TODO: This can be changed to GLYPH_ICON.getPropertyValue(vertex) once ENTITY_IMAGE_URL is added
        return vertex.getPropertyValue(GLYPH_ICON.getKey()) != null;
    }

    public String getColor() {
        return COLOR.getPropertyValue(vertex);
    }

    public String getDisplayName() {
        return DISPLAY_NAME.getPropertyValue(vertex);
    }

    public String getDisplayType() {
        return DISPLAY_TYPE.getPropertyValue(vertex);
    }

    public Vertex getVertex() {
        return this.vertex;
    }

    public JSONObject toJson() {
        try {
            JSONObject result = new JSONObject();
            result.put("id", getId());
            result.put("title", getTitle());
            result.put("displayName", getDisplayName());
            if (getDisplayType() != null) {
                result.put("displayType", getDisplayType());
            }
            if (this.parentConceptVertex != null) {
                result.put("parentConcept", this.parentConceptVertex.getId().toString());
            }
            if (getDisplayName() != null) {
                result.put("pluralDisplayName", English.plural(getDisplayName()));
            }
            if (hasGlyphIconResource()) {
                result.put("glyphIconHref", "resource?id=" + URLEncoder.encode(getId(), "utf8"));
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

    public Concept setProperty(String propertyName, Object propertyValue, Visibility visibility) {
        vertex.setProperty(propertyName, propertyValue, visibility);
        return this;
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", getDisplayName(), getTitle());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + (this.vertex != null ? this.vertex.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Concept other = (Concept) obj;
        if (this.vertex != other.vertex && (this.vertex == null || !this.vertex.equals(other.vertex))) {
            return false;
        }
        return true;
    }

    public static JSONArray toJsonConcepts(Iterable<Concept> concepts) {
        JSONArray conceptsJson = new JSONArray();
        for (Concept concept : concepts) {
            conceptsJson.put(concept.toJson());
        }
        return conceptsJson;
    }
}
