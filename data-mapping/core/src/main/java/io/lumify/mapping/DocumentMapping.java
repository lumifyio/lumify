package io.lumify.mapping;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.io.InputStream;

/**
 * An interface for DocumentMappings.
 */
@JsonTypeInfo(include = As.PROPERTY, property = "type", use = Id.NAME)
public interface DocumentMapping {
    /**
     * Execute this mapping against the provided document, creating all vertices and edges
     * found.
     *
     * @param inputDoc the document to read
     * @param state the mapping State
     * @param vertexIdPrefix the prefix used when generating vertex IDs
     */
    void mapDocument(final InputStream inputDoc, final MappingState state, final String vertexIdPrefix);
}
