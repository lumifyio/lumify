package io.lumify.mapping.xform;

import java.math.BigInteger;
import java.util.Arrays;
import org.junit.runners.Parameterized.Parameters;

public class BigIntegerValueTransformerTest extends AbstractValueTransformerTest<BigInteger> {
    @Parameters(name="{index}: {0}->{1}")
    public static Iterable<Object[]> getTestValues() {
        return Arrays.asList(new Object[][] {
            { null, null },
            { "", null },
            { "\n \t\t \n", null },
            { "1", BigInteger.ONE },
            { "27", new BigInteger("27") },
            { "-42", new BigInteger("-42") },
            { "   \t  23\n", new BigInteger("23") },
            { "23/b", null },
            { "10/2", null },
            { "not a number", null }
        });
    }

    public BigIntegerValueTransformerTest(final String testVal, final BigInteger expected) {
        super(new BigIntegerValueTransformer(), testVal, expected);
    }
}
