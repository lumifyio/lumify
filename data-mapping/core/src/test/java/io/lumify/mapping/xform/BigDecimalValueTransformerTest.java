package io.lumify.mapping.xform;

import java.math.BigDecimal;
import java.util.Arrays;
import org.junit.runners.Parameterized.Parameters;

public class BigDecimalValueTransformerTest extends AbstractValueTransformerTest<BigDecimal> {
    @Parameters(name="{index}: {0}->{1}")
    public static Iterable<Object[]> getTestValues() {
        return Arrays.asList(new Object[][] {
            { null, null },
            { "", null },
            { "\n \t\t \n", null },
            { "1", BigDecimal.ONE },
            { "27.73692", new BigDecimal("27.73692") },
            { "-3.14", new BigDecimal("-3.14") },
            { "   \t  23.0\n", new BigDecimal("23.0") },
            { "23/b", null },
            { "10/2", null },
            { "not a number", null }
        });
    }

    public BigDecimalValueTransformerTest(final String testVal, final BigDecimal expected) {
        super(new BigDecimalValueTransformer(), testVal, expected);
    }
}
