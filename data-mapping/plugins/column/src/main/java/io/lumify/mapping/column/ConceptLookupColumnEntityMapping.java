package io.lumify.mapping.column;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.lumify.mapping.column.AbstractColumnDocumentMapping.Row;
import java.util.Map;

/**
 * An entity mapping that gets its Concept URI from the values in
 * an input column.
 */
@JsonTypeName("columnLookup")
public class ConceptLookupColumnEntityMapping extends AbstractColumnEntityMapping {
    /**
     * The ColumnValue resolving to the Concept URI.
     */
    private final ColumnValue<String> conceptColumn;

    /**
     * Create a new ConstantConceptColumnEntityMapping.
     * @param concept       the ColumnValue providing the concept IRI for this entity
     * @param props         the properties of this entity
     * @param required      is this entity required? null for default
     */
    @JsonCreator
    public ConceptLookupColumnEntityMapping(@JsonProperty("concept") final ColumnValue<String> concept,
                                            @JsonProperty("properties") final Map<String, ColumnValue<?>> props,
                                            @JsonProperty(value="required", required=false) final Boolean required) {
        super(props, required);
        checkNotNull(concept, "Concept column must be provided");
        this.conceptColumn = concept;
    }

    @JsonProperty("concept")
    public final ColumnValue<String> getConceptColumn() {
        return conceptColumn;
    }

    @Override
    protected String getConceptIRI(final Row row) {
        return conceptColumn.getValue(row);
    }
}
