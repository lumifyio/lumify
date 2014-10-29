package io.lumify.palantir.dataImport.formatFunctions;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class AddNumberCommasFormatFunctionTest {
    private AddNumberCommasFormatFunction fn;

    @Before
    public void before() {
        fn = new AddNumberCommasFormatFunction();
    }

    @Test
    public void testNonNumber() {
        String found = fn.format("test");
        assertEquals("test", found);
    }

    @Test
    public void testNonDecimalNumber() {
        String found = fn.format("12");
        assertEquals("12", found);

        found = fn.format("1234");
        assertEquals("1,234", found);
    }

    @Test
    public void testDecimalNumber() {
        String found = fn.format("12.3");
        assertEquals("12.3", found);

        found = fn.format("1234.56789");
        assertEquals("1,234.56789", found);
    }
}