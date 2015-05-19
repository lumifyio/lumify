package io.lumify.mapping.column;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.lumify.mapping.column.AbstractColumnDocumentMapping.Row;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A multi-column value that uses the string values
 * of one or more columns to create a key for mapping
 * to the desired value. If a mapping for the full key
 * is not found, columns will be removed from the end
 * of the key until either a match is discovered or
 * all columns are exhausted. If no match is found,
 * null will be returned. A default value can be set
 * by providing a mapping for the isEmpty string.
 */
@JsonTypeName("mappedMultiColumn")
public class MappedMultiColumnValue extends AbstractColumnValue<String> {
    /**
     * The default separator String.
     */
    public static final String DEFAULT_SEPARATOR = ":";

    /**
     * The columns whose values will be used to construct
     * the mapping keys.
     */
    private final List<ColumnValue<String>> keyColumns;

    /**
     * The configured separator character.
     */
    private final String separator;

    /**
     * The map of constructed keys to desired values.
     */
    private final Map<String, String> valueMap;

    @JsonCreator
    public MappedMultiColumnValue(@JsonProperty("keyColumns") final List<ColumnValue<String>> keyColumns,
                                  @JsonProperty(value="separator", required=false) final String separator,
                                  @JsonProperty("valueMap") final Map<String, String> valueMap,
                                  @JsonProperty(value="multiKey", required=false) final String multiKey) {
        super(multiKey);
        checkNotNull(keyColumns, "key columns must be provided");
        checkArgument(!keyColumns.isEmpty(), "key columns must be provided");
        checkNotNull(valueMap, "value map must be provided");
        checkArgument(!valueMap.isEmpty(), "value map must be provided");
        checkArgument(separator == null || !separator.trim().isEmpty(), "separator must be non-isEmpty if provided");
        this.keyColumns = Collections.unmodifiableList(new ArrayList<ColumnValue<String>>(keyColumns));
        this.separator = separator != null ? separator.trim() : DEFAULT_SEPARATOR;
        this.valueMap = Collections.unmodifiableMap(new HashMap<String, String>(valueMap));
    }

    @JsonProperty("keyColumns")
    public final List<ColumnValue<String>> getKeyColumns() {
        return keyColumns;
    }

    @JsonProperty("separator")
    public String getSeparator() {
        return separator;
    }

    @JsonProperty("valueMap")
    public Map<String, String> getValueMap() {
        return valueMap;
    }

    @Override
    public int getSortColumn() {
        return keyColumns.get(0).getSortColumn();
    }

    @Override
    public String getValue(final Row row) {
        String value = null;
        for (int length = keyColumns.size(); value == null && length >= 0; length--) {
            value = valueMap.get(buildKey(row, length));
        }
        return value;
    }

    private String buildKey(final Row row, final int length) {
        StringBuilder builder = new StringBuilder();
        String value;
        for (int idx = 0; idx < length; idx++) {
            try {
                value = keyColumns.get(idx).getValue(row);
            } catch (RuntimeException re) {
                // ignore column if any errors occur resolving its value
                value = null;
            }
            if (builder.length() > 0) {
                builder.append(separator);
            }
            builder.append(value != null ? value : "");
        }
        return builder.toString();
    }
}
