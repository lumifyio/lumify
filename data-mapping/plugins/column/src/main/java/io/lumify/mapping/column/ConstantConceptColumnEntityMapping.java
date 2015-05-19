package io.lumify.mapping.column;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.lumify.mapping.column.AbstractColumnDocumentMapping.Row;
import java.util.Map;

/**
 * A ColumnEntityMapping with a configured Concept URI.
 */
@JsonTypeName("constant")
public class ConstantConceptColumnEntityMapping extends AbstractColumnEntityMapping {
    /**
     * The configured concept IRI.
     */
    private final String conceptIRI;

    /**
     * Create a new ConstantConceptColumnEntityMapping.
     *
     * @param concept       the concept URI for this entity
     * @param props         the properties of this entity
     * @param required      is this entity required? null for default
     */
    public ConstantConceptColumnEntityMapping(@JsonProperty("conceptIRI") final String concept,
                                              @JsonProperty("properties") final Map<String, ColumnValue<?>> props,
                                              @JsonProperty(value="required", required=false) final Boolean required) {
        super(props, required);
        checkNotNull(concept, "Concept IRI must be provided");
        checkArgument(!concept.trim().isEmpty(), "Concept IRI must be provided");
        this.conceptIRI = concept.trim();
    }

    @Override
    protected String getConceptIRI(final Row row) {
        return conceptIRI;
    }

    @JsonProperty("conceptIRI")
    public final String getConceptIRI() {
        return conceptIRI;
    }
}
