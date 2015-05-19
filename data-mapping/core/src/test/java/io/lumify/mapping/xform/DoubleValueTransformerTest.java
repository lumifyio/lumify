package io.lumify.mapping.xform;

import java.util.Arrays;
import org.junit.runners.Parameterized.Parameters;

public class DoubleValueTransformerTest extends AbstractValueTransformerTest<Double> {
    @Parameters(name="{index}: {0}->{1}")
    public static Iterable<Object[]> getTestValues() {
        return Arrays.asList(new Object[][] {
            { null, null },
            { "", null },
            { "\n \t\t \n", null },
            { "1", 1.0d },
            { "27.73692", 27.73692d },
            { "-3.14", -3.14d },
            { "   \t  23.0\n", 23.0d },
            { "23/b", null },
            { "10/2", null },
            { "not a number", null }
        });
    }

    public DoubleValueTransformerTest(final String testVal, final Double expected) {
        super(new DoubleValueTransformer(), testVal, expected);
    }
}
