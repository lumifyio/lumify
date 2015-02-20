package io.lumify.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.lumify.core.exception.LumifyException;
import io.lumify.web.clientapi.model.ClientApiObject;
import io.lumify.web.clientapi.model.util.ObjectMapperFactory;

import java.io.IOException;

public class SourceInfo implements ClientApiObject {
    private final long startOffset;
    private final long endOffset;
    private final String vertexId;
    private final String snippet;
    private final String textPropertyKey;

    public SourceInfo(
            @JsonProperty("vertexId") String vertexId,
            @JsonProperty("textPropertyKey") String textPropertyKey,
            @JsonProperty("startOffset") long startOffset,
            @JsonProperty("endOffset") long endOffset,
            @JsonProperty("snippet") String snippet
    ) {
        this.vertexId = vertexId;
        this.textPropertyKey = textPropertyKey;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.snippet = snippet;
    }

    public long getStartOffset() {
        return this.startOffset;
    }

    public long getEndOffset() {
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

    public static SourceInfo fromString(String sourceInfoString) {
        try {
            if (sourceInfoString == null || sourceInfoString.length() == 0) {
                return null;
            }
            return ObjectMapperFactory.getInstance().readValue(sourceInfoString, SourceInfo.class);
        } catch (IOException e) {
            throw new LumifyException("Could not read value: " + sourceInfoString, e);
        }
    }
}
