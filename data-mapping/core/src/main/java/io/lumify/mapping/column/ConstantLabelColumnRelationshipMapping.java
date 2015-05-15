package io.lumify.mapping.column;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.lumify.mapping.column.AbstractColumnDocumentMapping.Row;
import org.securegraph.Vertex;

/**
 * A relationship mapping with a constant, configured label.
 */
@JsonTypeName("constant")
@JsonPropertyOrder({ "source", "target", "label" })
public class ConstantLabelColumnRelationshipMapping extends AbstractColumnRelationshipMapping {
    /**
     * The relationship label.
     */
    private final String relationshipLabel;

    /**
     * Create a new ConstantLabelColumnRelationshipMapping.
     * @param srcKey the source entity key
     * @param tgtKey the target entity key
     * @param label the relationship label
     */
    @JsonCreator
    public ConstantLabelColumnRelationshipMapping(@JsonProperty("source") final String srcKey,
            @JsonProperty("target") final String tgtKey,
            @JsonProperty("label") final String label) {
        super(srcKey, tgtKey);
        checkNotNull(label, "relationship label must be provided");
        checkArgument(!label.trim().isEmpty(), "relationship label must be provided");
        this.relationshipLabel = label.trim();
    }

    @JsonProperty("label")
    public final String getRelationshipLabel() {
        return relationshipLabel;
    }

    @Override
    protected String getLabel(final Vertex source, final Vertex target, final Row row) {
        return relationshipLabel;
    }
}
