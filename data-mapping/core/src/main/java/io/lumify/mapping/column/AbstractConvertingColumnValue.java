package io.lumify.mapping.column;

import io.lumify.mapping.column.AbstractColumnDocumentMapping.Row;
import io.lumify.mapping.xform.StringValueTransformer;
import io.lumify.mapping.xform.ValueTransformer;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Base class for implementations of ColumnValue that use a
 * ValueTransformer to transform an input string, resolved from
 * the input row, to the desired value for the column.
 * @param <T> the type of value retrieved by this ColumnValue
 */
public abstract class AbstractConvertingColumnValue<T> extends AbstractColumnValue<T> {
    /**
     * The value transformer.
     */
    private final ValueTransformer<T> valueXform;

    /**
     * Create a new AbstractConvertingColumnValue.
     * @param xform the value transformer
     * @param multiKey the optional multiKey
     */
    @SuppressWarnings("unchecked")
    protected AbstractConvertingColumnValue(final ValueTransformer<T> xform, final String multiKey) {
        super(multiKey);
        // default to String transformation; this may cause ClassCastExceptions if
        // mappings are configured incorrectly
        this.valueXform = xform != null ? xform : (ValueTransformer<T>) new StringValueTransformer();
    }

    /**
     * Retrieve the String value that will be provided to the value
     * transformation from the columns of the provided row.
     * @param row the input row
     * @return the String that will be converted to the desired type
     */
    protected abstract String resolveInputValue(final Row row);

    @Override
    public final T getValue(final Row row) {
        String strValue = resolveInputValue(row);
        return valueXform.transform(strValue);
    }

    @JsonProperty("xform")
    public final ValueTransformer<T> getValueXform() {
        return valueXform;
    }
}
