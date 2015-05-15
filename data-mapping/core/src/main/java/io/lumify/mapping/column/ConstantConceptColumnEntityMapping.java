package io.lumify.mapping.column;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import io.lumify.mapping.column.AbstractColumnDocumentMapping.Row;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

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
     * @param labelProperty the label property
     * @param required      is this entity required? null for default
     */
    public ConstantConceptColumnEntityMapping(@JsonProperty("conceptIRI") final String concept,
                                              @JsonProperty("properties") final Map<String, ColumnValue<?>> props,
                                              @JsonProperty("labelProperty") final String labelProperty,
                                              @JsonProperty(value="required", required=false) final Boolean required) {
        super(props, labelProperty, required);
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
