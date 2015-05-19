package io.lumify.mapping.column;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import io.lumify.mapping.column.AbstractColumnDocumentMapping.Row;
import io.lumify.mapping.xform.ValueTransformer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SingleColumnValueTest {
    private static final int TEST_COLUMN = 10;

    @Mock
    private ValueTransformer<Object> xform;
    @Mock
    private Row row;

    private SingleColumnValue<Object> instance;

    @Before
    public void setup() {
        instance = new SingleColumnValue<>(TEST_COLUMN, xform, null);
    }

    @Test
    public void testIllegalConstruction() {
        doTestConstructor("column < 0", -1, IllegalArgumentException.class);
    }

    @Test
    public void testLegalConstruction() {
        doTestConstructor("column == 0", 0);
        doTestConstructor("column > 0", 10);
    }

    @Test
    public void testResolveInputValue() {
        doTestResolveInputValue("null value", null);
        doTestResolveInputValue("isEmpty value", "");
        doTestResolveInputValue("whitespace value", "\n \t\t \n");
        doTestResolveInputValue("value passthrough", "foo");
        doTestResolveInputValue("out of bounds", new IndexOutOfBoundsException(), null);
    }

    private void doTestConstructor(final String testName, final int index, final Class<? extends Throwable> expError) {
        try {
            new SingleColumnValue(index, xform, null);
            fail(String.format("[%s]: Expected %s", testName, expError.getName()));
        } catch (Exception e) {
            assertTrue(String.format("[%s]: Expected %s, got %s", testName, expError.getName(), e.getClass().getName()),
                    expError.isAssignableFrom(e.getClass()));
        }
    }

    private void doTestConstructor(final String testName, final int index) {
        SingleColumnValue<Object> instance = new SingleColumnValue<>(index, xform, null);
        assertEquals(String.format("[%s]: ", testName), index, instance.getIndex());
        assertEquals(String.format("[%s]: ", testName), index, instance.getSortColumn());
    }

    private void doTestResolveInputValue(final String testName, final String value) {
        when(row.get(TEST_COLUMN)).thenReturn(value);
        assertEquals(String.format("[%s]: ", testName), value, instance.resolveInputValue(row));
    }

    private void doTestResolveInputValue(final String testName, final Throwable err, final String expValue) {
        when(row.get(TEST_COLUMN)).thenThrow(err);
        assertEquals(String.format("[%s]: ", testName), expValue, instance.resolveInputValue(row));
    }
}
