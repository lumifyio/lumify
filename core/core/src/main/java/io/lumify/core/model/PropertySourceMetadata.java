package io.lumify.core.model;

import org.json.JSONObject;

import java.io.Serializable;

public class PropertySourceMetadata implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int startOffset;
    private final int endOffset;
    private final String vertexId;
    private final String snippet;
    private final String textPropertyKey;

    public PropertySourceMetadata(String vertexId, String textPropertyKey, int startOffset, int endOffset, String snippet) {
        this.vertexId = vertexId;
        this.textPropertyKey = textPropertyKey;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.snippet = snippet;
    }

    public PropertySourceMetadata(JSONObject sourceObject) {
        this.vertexId = sourceObject.getString("vertexId");
        this.textPropertyKey = sourceObject.getString("textPropertyKey");
        this.startOffset = sourceObject.getInt("startOffset");
        this.endOffset = sourceObject.getInt("endOffset");
        this.snippet = sourceObject.getString("snippet");
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

    public String getSnippet() {
        return snippet;
    }

    public String getTextPropertyKey() {
        return textPropertyKey;
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("startOffset", getStartOffset());
        jsonObject.put("endOffset", getEndOffset());
        jsonObject.put("vertexId", getVertexId());
        jsonObject.put("snippet", getSnippet());
        jsonObject.put("textPropertyKey", getTextPropertyKey());
        return jsonObject;
    }
}
