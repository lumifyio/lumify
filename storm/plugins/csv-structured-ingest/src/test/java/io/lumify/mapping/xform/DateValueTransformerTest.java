package io.lumify.mapping.xform;

import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;

public class DateValueTransformerTest extends AbstractValueTransformerTest<Long> {
    private static final String TEST_FORMAT = "yyyyMMdd-HHmm";
    private static final String TEST_TIMEZONE_AMERICA_NEW_YORK = "America/New_York";
    private static final String TEST_TIMEZONE_ASIA_KUWAIT = "Asia/Kuwait";

    @Parameters(name = "{index}: [{0}][{3}] {1}->{2}")
    public static Iterable<Object[]> getTestValues() {
        return Arrays.asList(new Object[][]{
                {null, null, null, TEST_TIMEZONE_AMERICA_NEW_YORK},
                {null, "", null, TEST_TIMEZONE_AMERICA_NEW_YORK},
                {null, "\n \t\t \n", null, TEST_TIMEZONE_AMERICA_NEW_YORK},
                {null, "not a date", null, TEST_TIMEZONE_AMERICA_NEW_YORK},
                {"", "07/07/1979 17:03:10", 300229390000L, TEST_TIMEZONE_AMERICA_NEW_YORK},
                {"", "07/07/1979 17:03:10", 300204190000L, TEST_TIMEZONE_ASIA_KUWAIT},
                {"\n \t\t \n", "10/15/2012", 1350273600000L, TEST_TIMEZONE_AMERICA_NEW_YORK},
                {"\n \t\t \n", "10/15/2012", 1350248400000L, TEST_TIMEZONE_ASIA_KUWAIT},
                {TEST_FORMAT, null, null, TEST_TIMEZONE_AMERICA_NEW_YORK},
                {TEST_FORMAT, "", null, TEST_TIMEZONE_AMERICA_NEW_YORK},
                {TEST_FORMAT, "\n \t\t \n", null, TEST_TIMEZONE_AMERICA_NEW_YORK},
                {TEST_FORMAT, "not a date", null, TEST_TIMEZONE_AMERICA_NEW_YORK},
                {TEST_FORMAT, "20110116-1600", 1295211600000L, TEST_TIMEZONE_AMERICA_NEW_YORK},
                {TEST_FORMAT, "20110116-1600", 1295182800000L, TEST_TIMEZONE_ASIA_KUWAIT}
        });
    }

    public DateValueTransformerTest(final String format, final String testVal, final Long expected, final String timeZone) {
        super(new DateValueTransformer(format), testVal, expected, timeZone);
    }
}
