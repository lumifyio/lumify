package io.lumify.mapping.column;

import io.lumify.core.ingest.term.extraction.TermMention;
import org.securegraph.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import java.util.List;

/**
 * The mapping definition for an entity found in a columnar data store.
 */
@JsonTypeInfo(include = As.PROPERTY, property = "conceptUriType", use = Id.NAME, defaultImpl = ConstantConceptColumnEntityMapping.class)
@JsonSubTypes({
        @Type(ConceptLookupColumnEntityMapping.class),
        @Type(ConstantConceptColumnEntityMapping.class)
})
@JsonInclude(Include.NON_EMPTY)
public interface ColumnEntityMapping extends Comparable<ColumnEntityMapping> {
    /**
     * Get the index of the sort column for this mapping.
     *
     * @return the index of the column that should be used for sorting this mapping
     */
    @JsonIgnore
    int getSortColumn();

    /**
     * Is this entity required?
     *
     * @return <code>true</code> if the entity is required
     */
    boolean isRequired();

    /**
     * Generate a TermMention, with all associated properties, from the columns
     * of a row in a columnar document.
     *
     * @param row        the columns of the input row
     * @param offset     the current document offset
     * @param processId  the ID of the process reading this document
     * @param visibility
     * @return the generated TermMention
     */
    TermMention mapTerm(final List<String> row, final int offset, final String processId, String propertyKey, Visibility visibility);
}
