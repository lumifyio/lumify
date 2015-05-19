package io.lumify.mapping.xform;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.math.BigInteger;

/**
 * This class attempts to parse an input string as a BigInteger,
 * returning the resulting value or null if it cannot be parsed.
 */
@JsonTypeName("bigInteger")
public class BigIntegerValueTransformer implements ValueTransformer<BigInteger> {
    @Override
    public BigInteger transform(final String input) {
        BigInteger value;
        try {
            value = new BigInteger(input.trim());
        } catch (NumberFormatException nfe) {
            value = null;
        } catch (NullPointerException npe) {
            value = null;
        }
        return value;
    }
}
