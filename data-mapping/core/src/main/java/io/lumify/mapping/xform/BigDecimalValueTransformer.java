package io.lumify.mapping.xform;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.math.BigDecimal;

/**
 * This class attempts to parse an input string as a BigDecimal,
 * returning the resulting value or null if it cannot be parsed.
 */
@JsonTypeName("bigDecimal")
public class BigDecimalValueTransformer implements ValueTransformer<BigDecimal> {
    @Override
    public BigDecimal transform(final String input) {
        BigDecimal value;
        try {
            value = new BigDecimal(input.trim());
        } catch (NumberFormatException nfe) {
            value = null;
        } catch (NullPointerException npe) {
            value = null;
        }
        return value;
    }
}
