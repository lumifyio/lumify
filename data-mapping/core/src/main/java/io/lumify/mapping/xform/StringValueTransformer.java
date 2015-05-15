package io.lumify.mapping.xform;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * This transformer returns all non-null, non-isEmpty input strings
 * as they are provided, returning null when null, isEmpty or whitespace
 * only inputs are encountered..
 */
@JsonTypeName("string")
public class StringValueTransformer implements ValueTransformer<String> {
    @Override
    public String transform(final String input) {
        return input != null && !input.trim().isEmpty() ? input : null;
    }
}
