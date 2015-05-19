package io.lumify.mapping.column;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.lumify.mapping.column.AbstractColumnDocumentMapping.Row;
import io.lumify.mapping.xform.ValueTransformer;

/**
 * A ColumnValue that transforms the string found in a single
 * column to a target value type.
 * @param <T> the type of the transformed value
 */
@JsonTypeName("single")
@JsonPropertyOrder({ "index", "xform" })
public class SingleColumnValue<T> extends AbstractConvertingColumnValue<T> {
    /**
     * The index of the column whose value will be retrieved.
     */
    private final int index;

    /**
     * Create a new SingleColumnValue.
     * @param index the column index
     * @param xform the value transformer
     * @param multiKey the optional multiKey
     */
    @JsonCreator
    public SingleColumnValue(@JsonProperty("index") final int index,
                             @JsonProperty(value="xform", required=false) final ValueTransformer<T> xform,
                             @JsonProperty(value="multiKey", required=false) final String multiKey) {
        super(xform, multiKey);
        checkArgument(index >= 0, "column index must be >= 0");
        this.index = index;
    }

    @JsonProperty("index")
    public final int getIndex() {
        return index;
    }

    @Override
    protected String resolveInputValue(final Row row) {
        String value;
        try {
            value = row.get(index);
        } catch (IndexOutOfBoundsException iobe) {
            value = null;
        }
        return value;
    }

    @Override
    public int getSortColumn() {
        return index;
    }
}
