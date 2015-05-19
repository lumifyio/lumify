package io.lumify.mapping.xform;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for value transformers that apply a mapping to the
 * input string to generate a value.
 * @param <T> the type of the transformed value
 */
@JsonPropertyOrder({ "defaultValue", "valueMap" })
public abstract class AbstractMappedValueTransformer<T> implements ValueTransformer<T> {
    /**
     * The value map.
     */
    private final Map<String, T> valueMap;

    /**
     * The default value.
     */
    private final T defaultValue;

    /**
     * Create a new AbstractMappedValueTransformer returning a default value
     * of <code>null</code> when the input string is not found.
     * @param valMap the value map
     */
    protected AbstractMappedValueTransformer(final Map<String, T> valMap) {
        this(valMap, null);
    }

    /**
     * Create a new AbstractMappedValueTransformer that returns the provided
     * default if the input string is not found.
     * @param valMap the value map
     * @param defVal the default value
     */
    protected AbstractMappedValueTransformer(final Map<String, T> valMap, final T defVal) {
        checkNotNull(valMap, "value map must be provided");
        checkArgument(!valMap.isEmpty(), "value  map must have at least one value");
        this.valueMap = Collections.unmodifiableMap(new HashMap<String, T>(valMap));
        this.defaultValue = defVal;
    }

    @Override
    public final T transform(final String input) {
        return valueMap.containsKey(input) ? valueMap.get(input) : defaultValue;
    }

    @JsonProperty("valueMap")
    public final Map<String, T> getValueMap() {
        return valueMap;
    }

    @JsonProperty("defaultValue")
    public final T getDefaultValue() {
        return defaultValue;
    }
}
