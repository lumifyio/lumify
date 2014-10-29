package io.lumify.palantir.dataImport.formatFunctions;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class AddSsnDashesFormatFunctionTest {
    private AddSsnDashesFormatFunction fn;

    @Before
    public void before() {
        fn = new AddSsnDashesFormatFunction();
    }

    @Test
    public void testNonSsn() {
        String found = fn.format("test");
        assertEquals("test", found);
    }

    @Test
    public void testSsn() {
        String found = fn.format("123121234");
        assertEquals("123-12-1234", found);
    }

    @Test
    public void testWrongNumberOfDigitsSsn() {
        String found = fn.format("12312123");
        assertEquals("12312123", found);
    }
}