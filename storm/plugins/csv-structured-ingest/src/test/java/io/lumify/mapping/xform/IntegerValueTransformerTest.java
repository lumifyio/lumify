package io.lumify.mapping.xform;

import java.util.Arrays;
import org.junit.runners.Parameterized.Parameters;

public class IntegerValueTransformerTest extends AbstractValueTransformerTest<Integer> {
    @Parameters(name="{index}: {0}->{1}")
    public static Iterable<Object[]> getTestValues() {
        return Arrays.asList(new Object[][] {
            { null, null },
            { "", null },
            { "\n \t\t \n", null },
            { "1", 1 },
            { "27", 27 },
            { "-42", -42 },
            { "   \t  23\n", 23 },
            { "23/b", null },
            { "10/2", null },
            { "not a number", null }
        });
    }

    public IntegerValueTransformerTest(final String testVal, final Integer expected) {
        super(new IntegerValueTransformer(), testVal, expected);
    }
}
