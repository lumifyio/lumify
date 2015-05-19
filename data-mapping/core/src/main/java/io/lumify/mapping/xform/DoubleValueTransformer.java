package io.lumify.mapping.xform;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * This class attempts to parse an input string as a Double,
 * returning the resulting value or null if it cannot be parsed.
 */
@JsonTypeName("double")
public class DoubleValueTransformer implements ValueTransformer<Double> {
    @Override
    public Double transform(final String input) {
        Double value;
        try {
            value = Double.valueOf(input.trim());
        } catch (NumberFormatException nfe) {
            value = null;
        } catch (NullPointerException npe) {
            value = null;
        }
        return value;
    }
}
