package io.lumify.mapping.csv;

import io.lumify.mapping.column.AbstractColumnDocumentMapping.Row;
import io.lumify.mapping.column.ColumnEntityMapping;
import io.lumify.mapping.column.ColumnRelationshipMapping;
import io.lumify.mapping.column.ColumnVertexRelationshipMapping;
import io.lumify.util.LineReader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;

import java.io.*;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CsvDocumentMapping.class})
public class CsvDocumentMappingTest {
    private static final String TEST_SUBJECT = "Test Subject";
    private static final int TEST_SKIP_ROWS = 1;
    private static final String ENTITY1_ID = "entity1";
    private static final String ENTITY2_ID = "entity2";
    private static final String ENTITY3_ID = "entity3";
    private static final int ENTITY1_IDX = 0;
    private static final int ENTITY2_IDX = 1;
    private static final int ENTITY3_IDX = 3;

    @Mock
    private Reader mappingReader;
    @Mock
    private ColumnEntityMapping entity1;
    @Mock
    private ColumnEntityMapping entity2;
    @Mock
    private ColumnEntityMapping entity3;
    @Mock
    private ColumnRelationshipMapping rel1;
    @Mock
    private ColumnRelationshipMapping rel2;

    private Map<String, ColumnEntityMapping> entityMap;
    private List<ColumnRelationshipMapping> relList;
    private List<ColumnVertexRelationshipMapping> vertexRelList;

    private CsvDocumentMapping instance;

    @Before
    public void setup() {
        entityMap = new HashMap<String, ColumnEntityMapping>();
        entityMap.put(ENTITY1_ID, entity1);
        entityMap.put(ENTITY2_ID, entity2);
        entityMap.put(ENTITY3_ID, entity3);
        relList = Arrays.asList(rel1, rel2);
        vertexRelList = Arrays.asList();

        when(entity1.getSortColumn()).thenReturn(ENTITY1_IDX);
        when(entity2.getSortColumn()).thenReturn(ENTITY2_IDX);
        when(entity3.getSortColumn()).thenReturn(ENTITY3_IDX);
        when(entity1.compareTo(entity1)).thenReturn(0);
        when(entity1.compareTo(entity2)).thenReturn(-1);
        when(entity1.compareTo(entity3)).thenReturn(-1);
        when(entity2.compareTo(entity1)).thenReturn(1);
        when(entity2.compareTo(entity2)).thenReturn(0);
        when(entity2.compareTo(entity3)).thenReturn(-1);
        when(entity3.compareTo(entity1)).thenReturn(1);
        when(entity3.compareTo(entity2)).thenReturn(1);
        when(entity3.compareTo(entity3)).thenReturn(0);

        // instance must be configured AFTER comparisons are set up for entity mocks
        instance = new CsvDocumentMapping(TEST_SUBJECT, TEST_SKIP_ROWS, entityMap, relList, vertexRelList);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testIllegalConstruction() {
        doConstructorTest("negative skipRows", -1, IllegalArgumentException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testLegalConstruction() {
        doConstructorTest("null skipRows", null, 0);
        doConstructorTest("0 skipRows", 0);
        doConstructorTest(">0 skipRows", 10);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void ingestDocument() throws Exception {
        InputStream testIn = mock(InputStream.class);
        Writer testOut = mock(Writer.class);
        InputStreamReader inReader = mock(InputStreamReader.class);
        CsvListReader reader = mock(CsvListReader.class);
        CsvListWriter writer = mock(CsvListWriter.class);

        PowerMockito.whenNew(InputStreamReader.class).withArguments(testIn).thenReturn(inReader);
        PowerMockito.whenNew(CsvListReader.class).withArguments(inReader, CsvPreference.EXCEL_PREFERENCE).thenReturn(reader);
        PowerMockito.whenNew(CsvListWriter.class).withArguments(testOut, CsvPreference.EXCEL_PREFERENCE).thenReturn(writer);

        List<String> line1 = mock(List.class, "line1");
        List<String> line2 = mock(List.class, "line2");
        List<String> line3 = mock(List.class, "line3");

        when(reader.read())
                .thenReturn(line1)
                .thenReturn(line2)
                .thenReturn(line3)
                .thenReturn(null);

        instance.ingestDocument(testIn, testOut);

        PowerMockito.verifyNew(InputStreamReader.class).withArguments(testIn);
        PowerMockito.verifyNew(CsvListReader.class).withArguments(inReader, CsvPreference.EXCEL_PREFERENCE);
        PowerMockito.verifyNew(CsvListWriter.class).withArguments(testOut, CsvPreference.EXCEL_PREFERENCE);
        verify(writer).write(line1);
        verify(writer).write(line2);
        verify(writer).write(line3);
        verify(writer).close();
    }

    @Test(expected = NoSuchElementException.class)
    public void testCsvRowIterator_EmptyDocument() throws Exception {
        when(mappingReader.read()).thenReturn(-1);
        Iterator<Row> iter = instance.getRows(mappingReader).iterator();
        assertFalse(iter.hasNext());
        iter.next();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCsvRowIterator_HasContent() throws Exception {
        LineReader lineReader = mock(LineReader.class);
        StringReader strReader = mock(StringReader.class);
        CsvListReader csvReader = mock(CsvListReader.class);
        String line1 = "line1";
        String line2 = "line2";
        List<String> fields1 = Arrays.asList("foo", "bar", "fizz", "buzz");
        List<String> fields2 = Arrays.asList("FOO", "BAR", "FIZZ", "BUZZ");
        int lineOffset1 = 30;
        int lineOffset2 = 60;

        PowerMockito.whenNew(LineReader.class).withArguments(mappingReader).thenReturn(lineReader);
        PowerMockito.whenNew(StringReader.class).withArguments(anyString()).thenReturn(strReader);
        PowerMockito.whenNew(CsvListReader.class).withArguments(strReader, CsvPreference.EXCEL_PREFERENCE).thenReturn(csvReader);

        when(lineReader.readLine()).thenReturn(line1, line2, null);
        when(lineReader.getOffset()).thenReturn(lineOffset1, lineOffset2);
        when(csvReader.read()).thenReturn(fields1, fields2);

        List<Row> expected = Arrays.asList(
                new Row(lineOffset1, fields1),
                new Row(lineOffset2, fields2)
        );

        Iterable<Row> rows = instance.getRows(mappingReader);
        List<Row> actual = new ArrayList<Row>();
        for (Row r : rows) {
            actual.add(r);
        }
        assertEquals(expected, actual);
        verify(lineReader).skipLines(TEST_SKIP_ROWS);
        PowerMockito.verifyNew(LineReader.class).withArguments(mappingReader);
        PowerMockito.verifyNew(StringReader.class).withArguments(line1);
        PowerMockito.verifyNew(StringReader.class).withArguments(line2);
        PowerMockito.verifyNew(CsvListReader.class, times(2)).withArguments(strReader, CsvPreference.EXCEL_PREFERENCE);
    }

    @SuppressWarnings("unchecked")
    private void doConstructorTest(final String testName, final Integer skipRows, final Class<? extends Throwable> expectedError) {
        try {
            new CsvDocumentMapping(TEST_SUBJECT, skipRows, entityMap, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
            fail(String.format("[%s] Expected %s.", testName, expectedError.getName()));
        } catch (Exception e) {
            assertTrue(String.format("[%s] expected %s, got %s.", testName, expectedError.getName(), e.getClass().getName()),
                    expectedError.isAssignableFrom(e.getClass()));
        }
    }

    private void doConstructorTest(final String testName, final Integer skipRows) {
        doConstructorTest(testName, skipRows, skipRows);
    }

    @SuppressWarnings("unchecked")
    private void doConstructorTest(final String testName, final Integer skipRows, final Integer expSkipRows) {
        CsvDocumentMapping mapping = new CsvDocumentMapping(TEST_SUBJECT, skipRows, entityMap, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        assertEquals(testName, expSkipRows.intValue(), mapping.getSkipRows());
    }
}
