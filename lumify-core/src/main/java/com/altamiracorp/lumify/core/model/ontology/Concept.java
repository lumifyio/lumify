package com.altamiracorp.lumify.core.model.ontology;

import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.Visibility;
import org.json.JSONException;
import org.json.JSONObject;

import static com.altamiracorp.lumify.core.util.ObjectHelper.toStringOrNull;

public class Concept {

    private final Vertex vertex;

    public Concept(Vertex vertex) {
        this.vertex = vertex;
    }

    public Object getId() {
        return this.vertex.getId();
    }

    public String getTitle() {
        return toStringOrNull(this.vertex.getPropertyValue(PropertyName.ONTOLOGY_TITLE.toString(), 0));
    }

    public String getGlyphIconResourceRowKey() {
        return toStringOrNull(this.vertex.getPropertyValue(PropertyName.GLYPH_ICON.toString(), 0));
    }

    public String getColor() {
        return toStringOrNull(this.vertex.getPropertyValue(PropertyName.COLOR.toString(), 0));
    }

    public String getDisplayName() {
        return toStringOrNull(this.vertex.getPropertyValue(PropertyName.DISPLAY_NAME.toString(), 0));
    }

    public String getDisplayType() {
        return toStringOrNull(this.vertex.getPropertyValue(PropertyName.DISPLAY_TYPE.toString(), 0));
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
            if (getGlyphIconResourceRowKey() != null) {
                result.put("glyphIconResourceRowKey", getGlyphIconResourceRowKey());
                result.put("glyphIconHref", "/resource/" + getGlyphIconResourceRowKey());
            }
            if (getColor() != null) {
                result.put("color", getColor());
            }
            return result;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public Concept setProperty(String propertyName, String propertyValue, Visibility visibility) {
        this.vertex.setProperties(this.vertex.getGraph().createProperty(propertyName, propertyValue, visibility));
        return this;
    }
}
