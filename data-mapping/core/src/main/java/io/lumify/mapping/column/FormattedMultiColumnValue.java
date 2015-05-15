package io.lumify.mapping.column;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import io.lumify.mapping.column.AbstractColumnDocumentMapping.Row;
import io.lumify.mapping.xform.ValueTransformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IllegalFormatConversionException;
import java.util.List;

/**
 * A ColumnValue that formats the values found in one or more
 * columns in the input row then uses a ValueTransformer on
 * the resulting string to extract the desired value for the
 * column.  When formatting, an isEmpty string will be provided
 * for any columns whose input values are null or outside the
 * provided row bounds.  If all inputs are null, isEmpty or
 * out of bounds, the value will be resolved as null.
 * @param <T> the desired output type
 */
@JsonTypeName("formattedMultiColumn")
@JsonPropertyOrder({ "indices", "format", "xform" })
public class FormattedMultiColumnValue<T> extends AbstractConvertingColumnValue<T> {
    /**
     * The column indices.  Values from these columns will be provided
     * to the format string in the order they appear in this list.
     */
    private final List<ColumnValue<?>> columns;

    /**
     * The format string that will be provided the values for each row.
     * This must be in a format usable by the String.format() method.
     */
    private final String format;

    /**
     * Create a new FormattedMultiColumnValue.
     * @param cols the columns that will be provided to the formatter
     * @param fmt the format string
     * @param xform the value transformer
     */
    @JsonCreator
    public FormattedMultiColumnValue(@JsonProperty("columns") final List<ColumnValue<?>> cols,
                                     @JsonProperty("format") final String fmt,
                                     @JsonProperty(value="xform", required=false) final ValueTransformer<T> xform,
                                     @JsonProperty(value="multiKey", required=false) final String multiKey) {
        super(xform, multiKey);
        checkNotNull(cols, "at least one column must be provided");
        checkArgument(!cols.isEmpty(), "at least one column must be provided");
        checkNotNull(fmt, "format string must be provided");
        checkArgument(!fmt.trim().isEmpty(), "format string must be provided");
        this.columns = Collections.unmodifiableList(new ArrayList<ColumnValue<?>>(cols));
        this.format = fmt;
    }

    @Override
    protected String resolveInputValue(final Row row) {
        int colCount = columns.size();
        Object[] values = new Object[colCount];
        boolean foundValue = false;
        for (int idx=0; idx < colCount; idx++) {
            values[idx] = columns.get(idx).getValue(row);
            foundValue = foundValue || values[idx] != null;
        }
        String fmtValue = null;
        if (foundValue) {
            try {
                fmtValue = String.format(format, values);
            } catch (IllegalFormatConversionException ifce) {
                fmtValue = null;
            }
        }
        return fmtValue;
    }

    @Override
    public int getSortColumn() {
        return columns.get(0).getSortColumn();
    }

    @JsonProperty("columns")
    public final List<ColumnValue<?>> getColumns() {
        return columns;
    }

    @JsonProperty("format")
    public final String getFormat() {
        return format;
    }
}
