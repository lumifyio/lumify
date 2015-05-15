package io.lumify.mapping.column;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.lumify.mapping.column.AbstractColumnDocumentMapping.Row;
import io.lumify.mapping.xform.ValueTransformer;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AbstractConvertingColumnValueTest {
    @Mock
    private Delegate delegate;
    @Mock
    private ValueTransformer<Object> xform;

    private AbstractConvertingColumnValue<Object> instance;

    @Before
    public void setup() {
        instance = new TestImpl<>(xform);
    }

    @Test
    public void testLegalConstruction() {
        assertEquals(xform, instance.getValueXform());
    }

    @Test
    public void testCompareTo() {
        doTestCompare("less than", 5, 7, -1);
        doTestCompare("equal to", 5, 5, 0);
        doTestCompare("greater than", 7, 5, 1);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetValue() {
        Row row = mock(Row.class);
        String resolvedValue = "resolved";
        String convertedValue = "converted";
        when(delegate.resolveInputValue(row)).thenReturn(resolvedValue);
        when(xform.transform(resolvedValue)).thenReturn(convertedValue);
        assertEquals(convertedValue, instance.getValue(row));
    }

    private void doTestCompare(final String testName, final int myCol, final int theirCol, final int expected) {
        AbstractConvertingColumnValue<?> other = mock(AbstractConvertingColumnValue.class);
        when(delegate.getSortColumn()).thenReturn(myCol);
        when(other.getSortColumn()).thenReturn(theirCol);

        String failFmt = "[%s]: Expected %s, was %d";
        int compare = instance.compareTo(other);
        if (expected < 0) {
            assertTrue(String.format(failFmt, testName, "< 0", compare), compare < 0);
        } else if (expected == 0) {
            assertTrue(String.format(failFmt, testName, "== 0", compare), compare == 0);
        } else {
            assertTrue(String.format(failFmt, testName, "> 0", compare), compare > 0);
        }
    }

    private interface Delegate {
        String resolveInputValue(Row row);
        int getSortColumn();
    }

    private class TestImpl<T> extends AbstractConvertingColumnValue<T> {
        public TestImpl(ValueTransformer<T> xform) {
            super(xform, null);
        }

        @Override
        protected String resolveInputValue(Row row) {
            return delegate.resolveInputValue(row);
        }

        @Override
        public int getSortColumn() {
            return delegate.getSortColumn();
        }
    }
}
