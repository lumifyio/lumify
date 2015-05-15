package io.lumify.mapping.column;

/**
 * Base class for ColumnValue implementations supporting configuration
 * of a constant key for multiply valued properties.
 * @param <T> the type of the property value
 */
public abstract class AbstractColumnValue<T> implements ColumnValue<T> {
    /**
     * The configured multi key.
     */
    private final String multiKey;

    /**
     * Create a new AbstractColumnValue with the given multiKey.
     * @param mKey the multiKey
     */
    protected AbstractColumnValue(final String mKey) {
        multiKey = mKey != null && !mKey.trim().isEmpty() ? mKey.trim() : null;
    }

    @Override
    public final String getMultiKey() {
        return multiKey;
    }

    @Override
    public int compareTo(final ColumnValue<?> o) {
        return getSortColumn() - o.getSortColumn();
    }
}
