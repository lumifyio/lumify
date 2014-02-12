package com.altamiracorp.lumify.core.model;

import org.json.JSONObject;

import java.io.Serializable;

public class PropertySourceMetadata implements Serializable{
    private static final long serialVersionUID = 1L;
    public static final String PROPERTY_SOURCE_METADATA = "sourceMetadata";
    public final int startOffset;
    public final int endOffset;
    public final String vertexId;

    public PropertySourceMetadata(int startOffset, int endOffset, String vertexId) {
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.vertexId = vertexId;
    }

    public int getStartOffset() {
        return this.startOffset;
    }

    public int getEndOffset() {
        return this.endOffset;
    }

    public String getVertexId() {
        return this.vertexId;
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("startOffset", getStartOffset());
        jsonObject.put("endOffset", getEndOffset());
        jsonObject.put("vertexId", getVertexId());
        return jsonObject;
    }
}
