package io.lumify.mapping.column;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import io.lumify.mapping.column.AbstractColumnDocumentMapping.Row;
import java.util.Map;
import org.securegraph.Vertex;

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
     * Define the relationship defined by this mapping between the provided
     * entities.
     * @param entities the map of entity keys to entities resolved from the current input row
     * @param row the current row
     * @return the relationship defined by this mapping or <code>null</code>
     * if it could not be created
     */
    RelationshipDef defineRelationship(final Map<String, Vertex> entities, final Row row);

    /**
     * Encapsulates the values required to build a relationship.
     */
    final class RelationshipDef {
        private final String label;
        private final Vertex source;
        private final Vertex target;

        public RelationshipDef(final String lbl, final Vertex src, final Vertex tgt) {
            checkNotNull(lbl, "label must be provided");
            checkNotNull(src, "source vertex must be provided");
            checkNotNull(tgt, "target vertex must be provided");
            label = lbl;
            source = src;
            target = tgt;
        }

        public String getLabel() {
            return label;
        }

        public Vertex getSource() {
            return source;
        }

        public Vertex getTarget() {
            return target;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            RelationshipDef that = (RelationshipDef) o;

            if (!label.equals(that.label)) return false;
            if (!source.equals(that.source)) return false;
            return target.equals(that.target);

        }

        @Override
        public int hashCode() {
            int result = label.hashCode();
            result = 31 * result + source.hashCode();
            result = 31 * result + target.hashCode();
            return result;
        }
    }
}
