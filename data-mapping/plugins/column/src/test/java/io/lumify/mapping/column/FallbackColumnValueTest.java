package io.lumify.mapping.column;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import io.lumify.mapping.column.AbstractColumnDocumentMapping.Row;
import io.lumify.mapping.predicate.MappingPredicate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FallbackColumnValueTest {
    private static final String PRIMARY_VAL = "primaryValue";
    private static final String FALLBACK_VAL = "fallbackValue";

    @Mock
    private ColumnValue<Object> primary;
    @Mock
    private ColumnValue<Object> fallback;
    @Mock
    private MappingPredicate<Object> fallbackIf;

    @Mock
    private Row row;

    private FallbackColumnValue<Object> instance;

    @Before
    public void setup() {
        instance = new FallbackColumnValue<>(primary, fallback, fallbackIf, null);
    }

    @Test
    public void testIllegalConstruction() {
        doTestConstructor("null primary", null, fallback, fallbackIf, NullPointerException.class);
        doTestConstructor("null fallback", primary, null, fallbackIf, NullPointerException.class);
    }

    @Test
    public void testLegalConstruction() {
        doTestConstructor("with condition", primary, fallback, fallbackIf);
        doTestConstructor("no condition", primary, fallback, null);
    }

    @Test
    public void testGetSortColumn() {
        int col = 27;
        when(primary.getSortColumn()).thenReturn(col);
        assertEquals(col, instance.getSortColumn());
    }

    @Test
    public void testGetValue_PrimaryValid() {
        when(primary.getValue(row)).thenReturn(PRIMARY_VAL);
        when(fallbackIf.matches(PRIMARY_VAL)).thenReturn(false);
        assertEquals(PRIMARY_VAL, instance.getValue(row));
    }

    @Test
    public void testGetValue_PrimaryNull() {
        when(primary.getValue(row)).thenReturn(null);
        when(fallback.getValue(row)).thenReturn(FALLBACK_VAL);
        assertEquals(FALLBACK_VAL, instance.getValue(row));
        verify(fallbackIf, never()).matches(any());
    }

    @Test
    public void testGetValue_PrimaryInvalid() {
        when(primary.getValue(row)).thenReturn(PRIMARY_VAL);
        when(fallback.getValue(row)).thenReturn(FALLBACK_VAL);
        when(fallbackIf.matches(PRIMARY_VAL)).thenReturn(true);
        assertEquals(FALLBACK_VAL, instance.getValue(row));
    }

    @Test
    public void testGetValue_PrimaryIllegalArg() {
        when(primary.getValue(row)).thenThrow(new IllegalArgumentException());
        when(fallback.getValue(row)).thenReturn(FALLBACK_VAL);
        assertEquals(FALLBACK_VAL, instance.getValue(row));
        verify(fallbackIf, never()).matches(any());
    }

    @Test
    public void testGetValue_FallbackNull() {
        when(primary.getValue(row)).thenReturn(null);
        when(fallback.getValue(row)).thenReturn(null);
        assertNull(instance.getValue(row));
        verify(fallbackIf, never()).matches(any());
    }

    @Test
    public void testGetValue_FallbackIllegalArg() {
        when(primary.getValue(row)).thenReturn(null);
        when(fallback.getValue(row)).thenThrow(new IllegalArgumentException());
        assertNull(instance.getValue(row));
        verify(fallbackIf, never()).matches(any());
    }

    private <T> void doTestConstructor(final String testName, final ColumnValue<T> primVal, final ColumnValue<T> fbVal,
            final MappingPredicate<T> pred, final Class<? extends Throwable> expError) {
        try {
            new FallbackColumnValue<T>(primVal, fbVal, pred, null);
            fail(String.format("[%s]: Expected %s", testName, expError.getName()));
        } catch (Exception e) {
            assertTrue(String.format("[%s]: Expected %s, got %s", testName, expError.getName(), e.getClass().getName()),
                    expError.isAssignableFrom(e.getClass()));
        }
    }

    private <T> void doTestConstructor(final String testName, final ColumnValue<T> primVal, final ColumnValue<T> fbVal,
            final MappingPredicate<T> pred) {
        FallbackColumnValue<T> col = new FallbackColumnValue<T>(primVal, fbVal, pred, null);
        String msg = String.format("[%s]: ", testName);
        assertEquals(msg, primVal, col.getPrimaryColumn());
        assertEquals(msg, fbVal, col.getFallbackColumn());
        assertEquals(msg, pred, col.getFallbackIf());
    }
}
