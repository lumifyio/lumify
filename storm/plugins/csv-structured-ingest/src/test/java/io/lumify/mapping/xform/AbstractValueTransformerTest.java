package io.lumify.mapping.xform;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public abstract class AbstractValueTransformerTest<V> {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(AbstractValueTransformerTest.class);
    private final String input;
    private final V expected;
    private final ValueTransformer<V> xform;
    private final String timeZone;
    private TimeZone defaultTimeZone;

    protected AbstractValueTransformerTest(ValueTransformer<V> xform, String input, V expected) {
        this(xform, input, expected, null);
    }

    public AbstractValueTransformerTest(ValueTransformer<V> xform, String input, V expected, String timeZone) {
        this.xform = xform;
        this.input = input;
        this.expected = expected;
        this.timeZone = timeZone;
    }

    @Before
    public void before() {
        if (timeZone != null) {
            defaultTimeZone = TimeZone.getDefault();
            TimeZone newTimeZone = TimeZone.getTimeZone(timeZone);
            LOGGER.info("changing timezone for test: %s -> %s", defaultTimeZone, newTimeZone);
            TimeZone.setDefault(newTimeZone);
        }
    }

    @After
    public void after() {
        if (defaultTimeZone != null) {
            TimeZone.setDefault(defaultTimeZone);
        }
    }

    @Test
    public void testTransform() {
        assertEquals(expected, xform.transform(input));
    }
}
