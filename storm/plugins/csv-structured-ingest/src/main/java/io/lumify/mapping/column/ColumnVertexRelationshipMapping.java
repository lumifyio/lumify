package io.lumify.mapping.column;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.lumify.core.ingest.term.extraction.TermMention;
import io.lumify.core.ingest.term.extraction.VertexRelationship;
import org.securegraph.Visibility;

import java.util.List;
import java.util.Map;

@JsonTypeInfo(include = JsonTypeInfo.As.PROPERTY, property = "labelType", use = JsonTypeInfo.Id.NAME, defaultImpl = ConstantLabelColumnVertexRelationshipMapping.class)
@JsonSubTypes({
        @JsonSubTypes.Type(ConstantLabelColumnVertexRelationshipMapping.class)
})
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public interface ColumnVertexRelationshipMapping {
    VertexRelationship createVertexRelationship(Map<String, TermMention> termMap, List<String> columns, Visibility visibility);
}
