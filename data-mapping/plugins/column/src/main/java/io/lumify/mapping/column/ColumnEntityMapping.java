package io.lumify.mapping.column;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import io.lumify.mapping.MappingState;
import io.lumify.mapping.column.AbstractColumnDocumentMapping.Row;
import org.securegraph.VertexBuilder;

/**
 * The mapping definition for an entity found in a columnar data store.
 */
@JsonTypeInfo(include = As.PROPERTY, property = "conceptIRIType", use = Id.NAME, defaultImpl = ConstantConceptColumnEntityMapping.class)
@JsonSubTypes({
        @Type(ConceptLookupColumnEntityMapping.class),
        @Type(ConstantConceptColumnEntityMapping.class)
})
@JsonInclude(Include.NON_EMPTY)
public interface ColumnEntityMapping {
    /**
     * Is this entity required?
     *
     * @return <code>true</code> if the entity is required
     */
    boolean isRequired();

    /**
     * Get a unique hash for the Vertex that will be generated when this mapping
     * is applied to the provided row. This hash will be used to determine if
     * a new Vertex needs to be created or if the desired Vertex already exists.
     * @param row the columns of the row
     * @return a unique hash identifying the Vertex
     */
    String getVertexHash(final Row row);

    /**
     * Generate a Vertex, with all associated properties, from the columns of
     * a row in a columnar document.
     * @param row the row
     * @param builder the VertexBuilder
     * @param state the mapping state
     * @return the generated Vertex
     */
    void createVertex(final Row row, final VertexBuilder builder, final MappingState state);
}
