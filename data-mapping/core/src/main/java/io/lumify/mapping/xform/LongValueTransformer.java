package io.lumify.mapping.xform;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * This class attempts to parse an input string as a Long,
 * returning the resulting value or null if it cannot be parsed.
 */
@JsonTypeName("long")
public class LongValueTransformer implements ValueTransformer<Long> {
    @Override
    public Long transform(final String input) {
        Long value;
        try {
            value = Long.valueOf(input.trim());
        } catch (NumberFormatException nfe) {
            value = null;
        } catch (NullPointerException npe) {
            value = null;
        }
        return value;
    }
}
