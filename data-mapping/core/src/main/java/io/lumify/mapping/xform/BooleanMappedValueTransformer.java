package io.lumify.mapping.xform;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Map;

/**
 * A value transformer that maps input strings to output Booleans.
 */
@JsonTypeName("mappedBoolean")
public class BooleanMappedValueTransformer extends AbstractMappedValueTransformer<Boolean> {
    /**
     * Create a new BooleanMappedValueTransformer.
     * @param valueMap the map of input strings to output values
     */
    public BooleanMappedValueTransformer(final Map<String, Boolean> valueMap) {
        super(valueMap);
    }

    /**
     * Create a new BooleanMappedValueTransformer that uses the provided default value
     * if an input string isn't mapped.
     * @param valueMap the map of input strings to output values
     * @param defaultValue the default value
     */
    @JsonCreator
    public BooleanMappedValueTransformer(@JsonProperty("valueMap") final Map<String, Boolean> valueMap,
            @JsonProperty(value="defaultValue", required=false) final Boolean defaultValue) {
        super(valueMap, defaultValue);
    }
}
