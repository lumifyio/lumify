package com.altamiracorp.lumify.core.model.ontology;

import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.tinkerpop.blueprints.Vertex;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class Concept extends GraphVertex {

    public abstract String getId();

    public abstract String getTitle();

    public abstract String getGlyphIconResourceRowKey();

    public abstract String getColor();

    public abstract String getDisplayName();

    public abstract String getDisplayType();

    public abstract Vertex getVertex();

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
}
