package com.altamiracorp.lumify.core.model.ontology;

import static com.altamiracorp.lumify.core.model.ontology.OntologyLumifyProperties.*;
import static com.altamiracorp.lumify.core.model.properties.LumifyProperties.*;

import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.Visibility;
import org.json.JSONException;
import org.json.JSONObject;

public class Concept {

    private final Vertex vertex;

    public Concept(Vertex vertex) {
        this.vertex = vertex;
    }

    public String getId() {
        return vertex.getId().toString();
    }

    public String getTitle() {
        return ONTOLOGY_TITLE.getPropertyValue(vertex);
    }

    public boolean hasGlyphIconResource() {
        // TO-DO: This can be changed to GLYPH_ICON.getPropertyValue(vertex) once ENTITY_IMAGE_URL is added
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
            result.put("displayType", getDisplayType());
            if (hasGlyphIconResource()) {
                result.put("glyphIconHref", "/resource/" + getId());
            }
            if (getColor() != null) {
                result.put("color", getColor());
            }
            return result;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public Concept setProperty(String propertyName, Object propertyValue, Visibility visibility) {
        vertex.setProperty(propertyName, propertyValue, visibility);
        return this;
    }
}
