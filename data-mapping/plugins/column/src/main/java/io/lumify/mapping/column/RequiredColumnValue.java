package io.lumify.mapping.column;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.lumify.mapping.column.AbstractColumnDocumentMapping.Row;

/**
 * A decorator for other ColumnValue implementations that
 * throws an Exception if the decorated ColumnValue returns
 * a null value.
 * @param <T> the type of value returned by the underlying ColumnValue
 */
@JsonTypeName("required")
public class RequiredColumnValue<T> implements ColumnValue<T> {
    /**
     * The decorated ColumnValue.
     */
    private final ColumnValue<T> delegate;

    /**
     * Create a new RequiredColumnValue.
     * @param delegate the required ColumnValue
     */
    @JsonCreator
    public RequiredColumnValue(@JsonProperty("column") ColumnValue<T> delegate) {
        checkNotNull(delegate, "Delegate ColumnValue must be provided");
        this.delegate = delegate;
    }

    @JsonProperty("column")
    public final ColumnValue<T> getDelegate() {
        return delegate;
    }

    @Override
    public int getSortColumn() {
        return delegate.getSortColumn();
    }

    @Override
    public T getValue(final Row row) {
        T value = delegate.getValue(row);
        if (value == null) {
            throw new LumifyColumnMappingException(row, null, this,
                    new IllegalArgumentException(String.format("Value is required for column %d", getSortColumn())));
        }
        return value;
    }

    @Override
    public String getMultiKey() {
        return delegate.getMultiKey();
    }

    @Override
    public int compareTo(final ColumnValue<?> o) {
        return delegate.compareTo(o);
    }
}
