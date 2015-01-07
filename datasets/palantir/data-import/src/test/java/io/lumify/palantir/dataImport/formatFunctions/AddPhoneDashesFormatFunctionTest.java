package io.lumify.palantir.dataImport.formatFunctions;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class AddPhoneDashesFormatFunctionTest {
    private AddPhoneDashesFormatFunction fn;

    @Before
    public void before() {
        fn = new AddPhoneDashesFormatFunction();
    }

    @Test
    public void testNonPhoneNumber() {
        String found = fn.format("test");
        assertEquals("test", found);
    }

    @Test
    public void testPhoneNumber10Digit() {
        String found = fn.format("1231231234");
        assertEquals("123-123-1234", found);
    }

    @Test
    public void testPhoneNumber7Digit() {
        String found = fn.format("1231234");
        assertEquals("123-1234", found);
    }

    @Test
    public void testWrongNumberOfDigitsPhoneNumber() {
        String found = fn.format("123123123");
        assertEquals("123123123", found);
    }
}