package io.lumify.mapping.column;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.lumify.core.ingest.term.extraction.TermMention;
import io.lumify.core.ingest.term.extraction.VertexRelationship;
import org.securegraph.Visibility;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@JsonTypeName("constant")
@JsonPropertyOrder({"source", "target", "label"})
public class ConstantLabelColumnVertexRelationshipMapping implements ColumnVertexRelationshipMapping {
    private final String sourceKey;
    private final int targetColumn;
    private final String relationshipLabel;

    @JsonCreator
    public ConstantLabelColumnVertexRelationshipMapping(
            @JsonProperty("source") final String srcKey,
            @JsonProperty("target") final int targetColumn,
            @JsonProperty("label") final String label) {
        checkNotNull(label, "relationship label must be provided");
        checkArgument(!label.trim().isEmpty(), "relationship label must be provided");
        this.sourceKey = srcKey;
        this.targetColumn = targetColumn;
        this.relationshipLabel = label.trim();
    }

    @JsonProperty("label")
    public final String getRelationshipLabel() {
        return relationshipLabel;
    }

    protected String getLabel() {
        return relationshipLabel;
    }

    @Override
    public VertexRelationship createVertexRelationship(final Map<String, TermMention> entities, final List<String> row, Visibility visibility) {
        VertexRelationship relationship = null;
        if (entities != null) {
            TermMention source = entities.get(sourceKey);
            String targetId = row.get(targetColumn);
            if (source != null && targetId != null) {
                String label = getLabel();
                if (label != null && !label.trim().isEmpty()) {
                    relationship = new VertexRelationship(source, targetId, label, visibility);
                }
            }
        }
        return relationship;
    }
}
