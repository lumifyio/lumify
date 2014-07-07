package io.lumify.mapping.column;

import io.lumify.core.ingest.term.extraction.TermMention;
import io.lumify.core.ingest.term.extraction.TermRelationship;
import org.securegraph.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.util.List;
import java.util.Map;

/**
 * The mapping definition for a relationship found in a columnar document.
 */
@JsonTypeInfo(include=As.PROPERTY, property="labelType", use=Id.NAME, defaultImpl=ConstantLabelColumnRelationshipMapping.class)
@JsonSubTypes({
    @Type(ConceptMappedColumnRelationshipMapping.class),
    @Type(ConstantLabelColumnRelationshipMapping.class)
})
@JsonInclude(Include.NON_EMPTY)
public interface ColumnRelationshipMapping {
    /**
     * Create the relationship defined by this mapping between the provided
     * entities.
     * @param entities the map of entity keys to entities resolved from the current input row
     * @param row the columns of the current row
     * @param visibility
     * @return the relationship defined by this mapping or <code>null</code>
     * if it could not be created
     */
    TermRelationship createRelationship(final Map<String, TermMention> entities, final List<String> row, Visibility visibility);
}
