package io.lumify.mapping.xform;

import java.util.Arrays;
import org.junit.runners.Parameterized.Parameters;

public class StringValueTransformerTest extends AbstractValueTransformerTest<String> {
    @Parameters(name="{index}: {0}->{0}")
    public static Iterable<Object[]> getTestValues() {
        return Arrays.asList(new Object[][] {
            { null, null },
            { "", null },
            { "\n \t\t \n", null },
            { "foo", "foo" },
            { "bar", "bar" },
            { "FiZZ", "FiZZ" },
            { "BUZZ", "BUZZ" }
        });
    }

    public StringValueTransformerTest(final String input, final String expected) {
        super(new StringValueTransformer(), input, expected);
    }
}
