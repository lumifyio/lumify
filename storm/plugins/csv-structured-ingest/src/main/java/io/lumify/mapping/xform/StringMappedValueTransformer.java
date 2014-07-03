package io.lumify.mapping.xform;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Map;

/**
 * A value transformer that maps input strings to output strings.
 */
@JsonTypeName("mappedString")
public class StringMappedValueTransformer extends AbstractMappedValueTransformer<String> {
    /**
     * Create a new StringMappedValueTransformer.
     * @param valueMap the map of input strings to output values
     */
    public StringMappedValueTransformer(final Map<String, String> valueMap) {
        super(valueMap);
    }

    /**
     * Create a new StringMappedValueTransformer that uses the provided default value
     * if an input string isn't mapped.
     * @param valueMap the map of input strings to output values
     * @param defaultValue the default value
     */
    @JsonCreator
    public StringMappedValueTransformer(@JsonProperty("valueMap") final Map<String, String> valueMap,
            @JsonProperty(value="defaultValue", required=false) final String defaultValue) {
        super(valueMap, defaultValue);
    }
}
