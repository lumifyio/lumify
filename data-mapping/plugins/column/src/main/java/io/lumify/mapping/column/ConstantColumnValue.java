package io.lumify.mapping.column;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.lumify.mapping.column.AbstractColumnDocumentMapping.Row;

/**
 * A ColumnValue that always returns the same value, regardless of provided inputs.
 */
@JsonTypeName("constant")
public class ConstantColumnValue<T> extends AbstractColumnValue<T> {
    /**
     * The value to return.
     */
    private final T value;

    /**
     * Create a new ConstantColumnValue.
     * @param val the value to return
     * @param multiKey the optional multiKey
     */
    @JsonCreator
    public ConstantColumnValue(@JsonProperty("value") final T val,
                               @JsonProperty(value="multiKey", required=false) final String multiKey) {
        super(multiKey);
        this.value = val;
    }

    @JsonProperty("value")
    public T getValue() {
        return value;
    }

    @Override
    public int getSortColumn() {
        return -1;
    }

    @Override
    public T getValue(final Row row) {
        return value;
    }
}
