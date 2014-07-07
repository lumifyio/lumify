package io.lumify.mapping.xform;

import java.util.Arrays;
import org.junit.runners.Parameterized.Parameters;

public class DateValueTransformerTest extends AbstractValueTransformerTest<Long> {
    private static final String TEST_FORMAT = "yyyyMMdd-HHmm";

    @Parameters(name="{index}: [{0}] {1}->{2}")
    public static Iterable<Object[]> getTestValues() {
        return Arrays.asList(new Object[][] {
            { null, null, null },
            { null, "", null },
            { null, "\n \t\t \n", null },
            { null, "not a date", null },
            { "", "07/07/1979 17:03:10", 300229390000L },
            { "\n \t\t \n", "10/15/2012", 1350273600000L },
            { TEST_FORMAT, null, null },
            { TEST_FORMAT, "", null },
            { TEST_FORMAT, "\n \t\t \n", null },
            { TEST_FORMAT, "not a date", null },
            { TEST_FORMAT, "20110116-1600", 1295211600000L }
        });
    }

    public DateValueTransformerTest(final String format, final String testVal, final Long expected) {
        super(new DateValueTransformer(format), testVal, expected);
    }
}
