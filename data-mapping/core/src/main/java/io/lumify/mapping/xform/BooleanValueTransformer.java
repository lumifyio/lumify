package io.lumify.mapping.xform;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * This class converts input strings to Boolean values.  Null,
 * isEmpty or whitespace only inputs will be converted to null
 * values.  All other inputs will be converted to FALSE unless
 * they are equivalent, ignoring case, to the string &quot;true&quot;.
 */
@JsonTypeName("boolean")
public class BooleanValueTransformer implements ValueTransformer<Boolean> {
    @Override
    public Boolean transform(final String input) {
        Boolean value = null;
        if (input != null && !input.trim().isEmpty()) {
            value = Boolean.parseBoolean(input.trim());
        }
        return value;
    }
}
