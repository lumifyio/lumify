package io.lumify.mapping.column;

import static io.lumify.mapping.column.MappedMultiColumnValue.DEFAULT_SEPARATOR;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.lumify.mapping.column.AbstractColumnDocumentMapping.Row;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MappedMultiColumnValueTest {
    private static final String NON_DEFAULT_SEPARATOR = "-";

    private static final Integer COL1_IDX = 1;
    private static final Integer COL2_IDX = 5;
    private static final Integer COL3_IDX = 7;

    private static final String VALUE1 = "value1";
    private static final String VALUE2 = "value2";
    private static final String VALUE3 = "value3";
    private static final String VALUE4 = "value4";
    private static final String VALUE5 = "value5";

    private static final String DEFAULT_LABEL = "defaultLabel";
    private static final String V1_LABEL = "valueLabel[1]";
    private static final String V12_LABEL = "valueLabel[1,2]";
    private static final String V123_LABEL = "valueLabel[1,2,3]";
    private static final String V1_3_LABEL = "valueLabel[1,-,3]";
    private static final String V_23_LABEL = "valueLabel[-,2,3]";

    private static final Map<String, String> DEF_SEP_NO_DEF_MAP = buildConceptMap(DEFAULT_SEPARATOR, false);
    private static final Map<String, String> DEF_SEP_WITH_DEF_MAP = buildConceptMap(DEFAULT_SEPARATOR, true);
    private static final Map<String, String> NON_DEF_SEP_NO_DEF_MAP = buildConceptMap(NON_DEFAULT_SEPARATOR, false);

    private static Map<String, String> buildConceptMap(final String sep, final boolean addDefault) {
        Map<String, String> map = new HashMap<>();
        if (addDefault) {
            map.put("", DEFAULT_LABEL);
        }
        map.put(buildKey(sep, VALUE1), V1_LABEL);
        map.put(buildKey(sep, VALUE1, VALUE2), V12_LABEL);
        map.put(buildKey(sep, VALUE1, VALUE2, VALUE3), V123_LABEL);
        map.put(buildKey(sep, VALUE1, "", VALUE3), V1_3_LABEL);
        map.put(buildKey(sep, "", VALUE2, VALUE3), V_23_LABEL);
        return Collections.unmodifiableMap(map);
    }

    private static String buildKey(final String sep, final String... values) {
        StringBuilder builder = new StringBuilder();
        for (String val : values) {
            if (builder.length() > 0) {
                builder.append(sep);
            }
            builder.append(val);
        }
        return builder.toString();
    }

    @Mock
    private ColumnValue<String> col1;
    @Mock
    private ColumnValue<String> col2;
    @Mock
    private ColumnValue<String> col3;

    private List<ColumnValue<String>> testKeyCols;

    @Before
    public void setup() {
        when(col1.getSortColumn()).thenReturn(COL1_IDX);
        when(col2.getSortColumn()).thenReturn(COL2_IDX);
        when(col3.getSortColumn()).thenReturn(COL3_IDX);
        testKeyCols = Arrays.asList(col1, col2, col3);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testIllegalConstruction() {
        doTestConstructor("null key columns", (List<ColumnValue<String>>) null, NullPointerException.class);
        doTestConstructor("isEmpty key columns", Collections.EMPTY_LIST, IllegalArgumentException.class);
        doTestConstructor("null value map", (Map<String, String>) null, NullPointerException.class);
        doTestConstructor("isEmpty value map", Collections.EMPTY_MAP, IllegalArgumentException.class);
        doTestConstructor("isEmpty separator", "", IllegalArgumentException.class);
        doTestConstructor("whitespace separator", "\n \t\t \n", IllegalArgumentException.class);
    }

    @Test
    public void testLegalConstruction() {
        doTestConstructor("null separator", null, DEFAULT_SEPARATOR);
        doTestConstructor("trimmed separator", NON_DEFAULT_SEPARATOR, NON_DEFAULT_SEPARATOR);
        doTestConstructor("untrimmed separator", "\t  " + NON_DEFAULT_SEPARATOR + "   \n", NON_DEFAULT_SEPARATOR);
    }

    @Test
    public void testGetConceptLabel() {
        Throwable ex = new IndexOutOfBoundsException();
        doTestGetValue("null values, no default", DEF_SEP_NO_DEF_MAP, DEFAULT_SEPARATOR, valMap(null, null, null), null);
        doTestGetValue("null values, with default", DEF_SEP_WITH_DEF_MAP, DEFAULT_SEPARATOR, valMap(null, null, null), DEFAULT_LABEL);
        doTestGetValue("exceptions, no default", DEF_SEP_NO_DEF_MAP, DEFAULT_SEPARATOR, valMap(ex, ex, ex), null);
        doTestGetValue("exceptions, with default", DEF_SEP_WITH_DEF_MAP, DEFAULT_SEPARATOR, valMap(ex, ex, ex), DEFAULT_LABEL);
        doTestGetValue("no mapping, no default", DEF_SEP_NO_DEF_MAP, DEFAULT_SEPARATOR, valMap(VALUE3, VALUE4, VALUE5), null);
        doTestGetValue("no mapping, with default", DEF_SEP_WITH_DEF_MAP, DEFAULT_SEPARATOR, valMap(VALUE3, VALUE4, VALUE5), DEFAULT_LABEL);
        doTestGetValue("v1,-,-", DEF_SEP_WITH_DEF_MAP, DEFAULT_SEPARATOR, valMap(VALUE1, null, null), V1_LABEL);
        doTestGetValue("v1,v2,-", NON_DEF_SEP_NO_DEF_MAP, NON_DEFAULT_SEPARATOR, valMap(VALUE1, VALUE2, null), V12_LABEL);
        doTestGetValue("v1,v2,v3", NON_DEF_SEP_NO_DEF_MAP, NON_DEFAULT_SEPARATOR, valMap(VALUE1, VALUE2, VALUE3), V123_LABEL);
        doTestGetValue("v1,-,v3", DEF_SEP_WITH_DEF_MAP, DEFAULT_SEPARATOR, valMap(VALUE1, null, VALUE3), V1_3_LABEL);
        doTestGetValue("v1,ex,v3", DEF_SEP_WITH_DEF_MAP, DEFAULT_SEPARATOR, valMap(VALUE1, ex, VALUE3), V1_3_LABEL);
        doTestGetValue("-,v2,v3", DEF_SEP_WITH_DEF_MAP, DEFAULT_SEPARATOR, valMap(null, VALUE2, VALUE3), V_23_LABEL);
        doTestGetValue("ex,v2,v3", DEF_SEP_WITH_DEF_MAP, DEFAULT_SEPARATOR, valMap(ex, VALUE2, VALUE3), V_23_LABEL);
        doTestGetValue("v1,v4,v5", DEF_SEP_NO_DEF_MAP, DEFAULT_SEPARATOR, valMap(VALUE1, VALUE4, VALUE5), V1_LABEL);
        doTestGetValue("v1,v2,v5", DEF_SEP_NO_DEF_MAP, DEFAULT_SEPARATOR, valMap(VALUE1, VALUE2, VALUE5), V12_LABEL);
    }

    private Map<ColumnValue<String>, Object> valMap(final Object col1val, final Object col2val, final Object col3val) {
        Map<ColumnValue<String>, Object> map = new HashMap<>();
        map.put(col1, col1val);
        map.put(col2, col2val);
        map.put(col3, col3val);
        return map;
    }

    @SuppressWarnings("unchecked")
    private void doTestGetValue(final String testName, final Map<String, String> valMap, final String sep,
            final Map<ColumnValue<String>, Object> colValMap, final String expValue) {
        Row row = mock(Row.class);
        for (Map.Entry<ColumnValue<String>, Object> entry : colValMap.entrySet()) {
            if (entry.getValue() instanceof Throwable) {
                when(entry.getKey().getValue(row)).thenThrow((Throwable) entry.getValue());
            } else {
                when(entry.getKey().getValue(row)).thenReturn((String) entry.getValue());
            }
        }
        MappedMultiColumnValue instance = new MappedMultiColumnValue(testKeyCols, sep, valMap, null);
        String value = instance.getValue(row);
        assertEquals(String.format("[%s]: ", testName), expValue, value);
    }

    private void doTestConstructor(final String testName, final List<ColumnValue<String>> cols, final Class<? extends Throwable> expError) {
        doTestConstructor(testName, cols, DEF_SEP_NO_DEF_MAP, null, expError);
    }

    private void doTestConstructor(final String testName, final Map<String, String> conMap, final Class<? extends Throwable> expError) {
        doTestConstructor(testName, testKeyCols, conMap, null, expError);
    }

    private void doTestConstructor(final String testName, final String sep, final Class<? extends Throwable> expError) {
        doTestConstructor(testName, testKeyCols, DEF_SEP_NO_DEF_MAP, sep, expError);
    }

    private void doTestConstructor(final String testName, final List<ColumnValue<String>> keyCols, final Map<String, String> valMap, final String sep,
            final Class<? extends Throwable> expError) {
        try {
            new MappedMultiColumnValue(keyCols, sep, valMap, null);
            fail(String.format("[%s]: Expected %s", testName, expError.getClass()));
        } catch (Exception e) {
            assertTrue(String.format("[%s]: Expected %s, got %s", testName, expError.getName(), e.getClass().getName()),
                    expError.isAssignableFrom(e.getClass()));
        }
    }

    private void doTestConstructor(final String testName, final String sep, final String expSep) {
        doTestConstructor(testName, testKeyCols, DEF_SEP_NO_DEF_MAP, sep, testKeyCols, DEF_SEP_NO_DEF_MAP, expSep);
    }

    private void doTestConstructor(final String testName, final List<ColumnValue<String>> keyCols, final Map<String, String> valMap,
            final String sep, final List<ColumnValue<String>> expKeyCols, final Map<String, String> expValMap, final String expSep) {
        MappedMultiColumnValue instance = new MappedMultiColumnValue(keyCols, sep, valMap, null);
        String msg = String.format("[%s]: ", testName);
        assertEquals(msg, expKeyCols, instance.getKeyColumns());
        assertEquals(msg, expValMap, instance.getValueMap());
        assertEquals(msg, expSep, instance.getSeparator());
    }
}
