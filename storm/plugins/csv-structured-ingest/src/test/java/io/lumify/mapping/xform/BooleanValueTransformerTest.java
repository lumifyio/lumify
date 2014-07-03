package io.lumify.mapping.xform;

import java.util.Arrays;
import org.junit.runners.Parameterized.Parameters;

public class BooleanValueTransformerTest extends AbstractValueTransformerTest<Boolean> {
    @Parameters(name="{index}: {0}->{1}")
    public static Iterable<Object[]> getTestValues() {
        return Arrays.asList(new Object[][] {
            { null, null },
            { "", null },
            { "\n \t\t \n", null },
            { "foo", Boolean.FALSE },
            { "bar", Boolean.FALSE },
            { "true", Boolean.TRUE },
            { "TRUE", Boolean.TRUE },
            { "\tTrUe   \n", Boolean.TRUE },
            { "true!", Boolean.FALSE },
            { "false", Boolean.FALSE },
            { "   asdf   ", Boolean.FALSE }
        });
    }

    public BooleanValueTransformerTest(final String testVal, final Boolean expected) {
        super(new BooleanValueTransformer(), testVal, expected);
    }
}
