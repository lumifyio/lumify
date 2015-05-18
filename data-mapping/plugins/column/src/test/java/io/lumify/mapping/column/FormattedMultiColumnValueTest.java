package io.lumify.mapping.column;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.lumify.mapping.column.AbstractColumnDocumentMapping.Row;
import io.lumify.mapping.xform.ValueTransformer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FormattedMultiColumnValueTest {
    private static final int COL1_IDX = 3;
    private static final int COL2_IDX = 7;
    private static final int COL3_IDX = 10;
    private static final String TEST_FORMAT = "%s::%d::%s";

    @Mock
    private ColumnValue<?> col1;
    @Mock
    private ColumnValue<?> col2;
    @Mock
    private ColumnValue<?> col3;
    @Mock
    private ValueTransformer<Object> xform;

    private List<ColumnValue<?>> testColumns;
    private FormattedMultiColumnValue<Object> instance;

    @Before
    @SuppressWarnings("unchecked")
    public void setup() {
        when(col1.getSortColumn()).thenReturn(COL1_IDX);
        when(col2.getSortColumn()).thenReturn(COL2_IDX);
        when(col3.getSortColumn()).thenReturn(COL3_IDX);
        testColumns = Arrays.asList(col1, col2, col3);
        instance = new FormattedMultiColumnValue<>(testColumns, TEST_FORMAT, xform, null);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testIllegalConstruction() {
        doTestConstructor("null columns", null, TEST_FORMAT, NullPointerException.class);
        doTestConstructor("isEmpty columns", Collections.EMPTY_LIST, TEST_FORMAT, IllegalArgumentException.class);
        doTestConstructor("null format", testColumns, null, NullPointerException.class);
        doTestConstructor("isEmpty format", testColumns, "", IllegalArgumentException.class);
        doTestConstructor("whitespace format", testColumns, "\n \t\t \n", IllegalArgumentException.class);
    }

    @Test
    public void testLegalConstruction() {
        assertEquals(testColumns, instance.getColumns());
        assertEquals(TEST_FORMAT, instance.getFormat());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetSortColumn() {
        List<ColumnValue<?>> unsorted = Arrays.asList(col2, col1, col3);
        FormattedMultiColumnValue<Object> unsortedColumns = new FormattedMultiColumnValue<>(unsorted, TEST_FORMAT, xform, null);
        assertEquals(testColumns.get(0).getSortColumn(), instance.getSortColumn());
        assertEquals(unsorted.get(0).getSortColumn(), unsortedColumns.getSortColumn());
    }

    @Test
    public void testResolveInputValue() {
        doTestResolveInputValue("all provided", "foo", 2, "bar", "foo::2::bar");
        doTestResolveInputValue("1, null, 3", "foo", null, "bar", "foo::null::bar");
        doTestResolveInputValue("1, 2, null", "foo", 2, null, "foo::2::null");
        doTestResolveInputValue("null, 2, null", null, 2, null, "null::2::null");
        doTestResolveInputValue("isEmpty, null, 3", "", null, "bar", "::null::bar");
        doTestResolveInputValue("null, null, null", null, null, null, null);
        doTestResolveInputValue("isEmpty, null, isEmpty", "", null, "", "::null::");
        doTestResolveInputValue("invalid format", "foo", "not a number", "bar", null);
    }

    private void doTestResolveInputValue(final String testName, final Object col1val, final Object col2val, final Object col3val,
            final String expected) {
        Row row = mock(Row.class);
        when(col1.getValue(row)).thenReturn(col1val);
        when(col2.getValue(row)).thenReturn(col2val);
        when(col3.getValue(row)).thenReturn(col3val);
        String resolved = instance.resolveInputValue(row);
        assertEquals(String.format("[%s]: ", testName), expected, resolved);
    }

    private void doTestConstructor(final String testName, final List<ColumnValue<?>> cols, final String fmt,
            final Class<? extends Throwable> expError) {
        try {
            new FormattedMultiColumnValue<>(cols, fmt, xform, null);
            fail(String.format("[%s]: Expected %s", testName, expError.getName()));
        } catch (Exception e) {
            assertTrue(String.format("[%s]: Expected %s, got %s", testName, expError.getName(), e.getClass().getName()),
                    expError.isAssignableFrom(e.getClass()));
        }
    }
}
