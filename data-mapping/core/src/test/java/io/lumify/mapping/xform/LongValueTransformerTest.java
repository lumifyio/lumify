package io.lumify.mapping.xform;

import java.util.Arrays;
import org.junit.runners.Parameterized.Parameters;

public class LongValueTransformerTest extends AbstractValueTransformerTest<Long> {
    @Parameters(name="{index}: {0}->{1}")
    public static Iterable<Object[]> getTestValues() {
        return Arrays.asList(new Object[][] {
            { null, null },
            { "", null },
            { "\n \t\t \n", null },
            { "1", 1L },
            { "27", 27L },
            { "-42", -42L },
            { "   \t  23\n", 23L },
            { "1234567890987654321", 1234567890987654321L },
            { "23/b", null },
            { "10/2", null },
            { "not a number", null }
        });
    }

    public LongValueTransformerTest(final String testVal, final Long expected) {
        super(new LongValueTransformer(), testVal, expected);
    }
}
