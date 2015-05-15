package io.lumify.mapping.column;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import io.lumify.mapping.column.AbstractColumnDocumentMapping.Row;
import java.util.List;

/**
 * A value retrieved from one or more columns of a row found in a
 * row/column data store, such as a CSV, TSV or similar spreadsheet
 * or data table representation.
 * @param <T> the type of value returned by this column
 */
@JsonTypeInfo(include=As.PROPERTY, property="type", use=Id.NAME, defaultImpl=SingleColumnValue.class)
@JsonSubTypes({
    @Type(ConstantColumnValue.class),
    @Type(FallbackColumnValue.class),
    @Type(FormattedMultiColumnValue.class),
    @Type(GeoCircleColumnValue.class),
    @Type(GeoPointColumnValue.class),
    @Type(MappedMultiColumnValue.class),
    @Type(RequiredColumnValue.class),
    @Type(SingleColumnValue.class)
})
@JsonInclude(Include.NON_EMPTY)
public interface ColumnValue<T> extends Comparable<ColumnValue<?>> {
    /**
     * Get the index into the data table used to sort this
     * column.
     * @return the sort column index; must be &gt;= 0
     */
    @JsonIgnore
    int getSortColumn();

    /**
     * @return the key used to store multiple values of this property
     */
    String getMultiKey();

    /**
     * Retrieve the value represented by this column configuration from
     * the provided row.
     * @param row the row
     * @return the value parsed from the input row for this column
     */
    T getValue(final Row row);
}
