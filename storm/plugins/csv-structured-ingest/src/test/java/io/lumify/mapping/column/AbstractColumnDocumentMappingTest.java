package io.lumify.mapping.column;

import io.lumify.core.ingest.term.extraction.TermExtractionResult;
import io.lumify.core.ingest.term.extraction.TermMention;
import io.lumify.core.ingest.term.extraction.TermRelationship;
import io.lumify.mapping.column.AbstractColumnDocumentMapping.Row;
import io.lumify.mapping.csv.CsvDocumentMapping;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.securegraph.Visibility;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CsvDocumentMapping.class})
public class AbstractColumnDocumentMappingTest {
    private static final String TEST_SUBJECT = "Test Subject";
    private static final String TEST_PROCESS_ID = "testProcess";
    private static final String LIVES_AT = "livesAt";
    private static final String KNOWS = "knows";
    private static final String ENTITY1_ID = "entity1";
    private static final String ENTITY2_ID = "entity2";
    private static final String ENTITY3_ID = "entity3";
    private static final int ENTITY1_IDX = 0;
    private static final int ENTITY2_IDX = 1;
    private static final int ENTITY3_IDX = 3;

    @Mock
    private Reader mappingReader;
    @Mock
    private Iterable<Row> rowIterable;
    @Mock
    private Iterator<Row> rowIter;
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

    @Mock
    private Delegate delegate;

    private AbstractColumnDocumentMapping instance;

    private Visibility visibility = new Visibility("");
    private List<ColumnVertexRelationshipMapping> vertexRelList;

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

        when(delegate.getRows(mappingReader)).thenReturn(rowIterable);
        when(rowIterable.iterator()).thenReturn(rowIter);

        // instance must be configured AFTER comparisons are set up for entity mocks
        instance = new TestImpl(TEST_SUBJECT, entityMap, relList, vertexRelList);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testIllegalConstruction() {
        doConstructorTest("null subject", (String) null, IllegalArgumentException.class);
        doConstructorTest("empty subject", "", IllegalArgumentException.class);
        doConstructorTest("whitespace subject", "\n \t\t \n", IllegalArgumentException.class);
        doConstructorTest("null entities", (Map) null, NullPointerException.class);
        doConstructorTest("empty entities", Collections.EMPTY_MAP, IllegalArgumentException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testLegalConstruction() {
        doConstructorTest("trimmed subject", TEST_SUBJECT);
        doConstructorTest("untrimmed subject", "\n \t" + TEST_SUBJECT + "\t \n", TEST_SUBJECT);
        doConstructorTest_Entities("multiple entities", entityMap);
        Map<String, ColumnEntityMapping> singleton = new HashMap<String, ColumnEntityMapping>();
        singleton.put(ENTITY1_ID, entity1);
        doConstructorTest_Entities("one entity", singleton);
        doConstructorTest_Rels("null relationships", Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        doConstructorTest_Rels("empty relationships", new ArrayList<ColumnRelationshipMapping>(), Collections.EMPTY_LIST);
    }

    @Test
    public void testMapDocument_EmptyDocument() throws Exception {
        when(rowIter.hasNext()).thenReturn(false);
        TermExtractionResult expected = new TermExtractionResult();
        TermExtractionResult actual = instance.mapDocument(mappingReader, TEST_PROCESS_ID, "", visibility);
        assertEquals(expected, actual);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMapDocument_NullColumns() throws Exception {
        TermMention line1entity1 = mock(TermMention.class, "l1t1");
        TermMention line1entity2 = mock(TermMention.class, "l1t2");
        TermMention line1entity3 = mock(TermMention.class, "l1t3");
        List<String> fields1 = Arrays.asList("foo", "bar", "fizz", "buzz");
        int lineOffset1 = 30;
        int line1Term1Offset = lineOffset1;
        int line1Term2Offset = line1Term1Offset + fields1.get(0).length() + 1;
        int line1Term3Offset = line1Term2Offset + fields1.get(1).length() + fields1.get(2).length() + 2;
        TermRelationship resolvedRel1 = new TermRelationship(line1entity1, line1entity2, LIVES_AT, visibility);
        TermRelationship resolvedRel2 = new TermRelationship(line1entity3, line1entity1, KNOWS, visibility);
        Map<String, TermMention> line1map = new HashMap<String, TermMention>();
        line1map.put(ENTITY1_ID, line1entity1);
        line1map.put(ENTITY2_ID, line1entity2);
        line1map.put(ENTITY3_ID, line1entity3);

        when(rowIter.hasNext()).thenReturn(true, false);
        when(rowIter.next()).thenReturn(new Row(lineOffset1, fields1));
        when(entity1.mapTerm(fields1, line1Term1Offset, TEST_PROCESS_ID, "", visibility)).thenReturn(line1entity1);
        when(entity2.mapTerm(fields1, line1Term2Offset, TEST_PROCESS_ID, "", visibility)).thenReturn(line1entity2);
        when(entity3.mapTerm(fields1, line1Term3Offset, TEST_PROCESS_ID, "", visibility)).thenReturn(line1entity3);
        when(rel1.createRelationship(line1map, fields1, visibility)).thenReturn(resolvedRel1);
        when(rel2.createRelationship(line1map, fields1, visibility)).thenReturn(resolvedRel2);

        List<TermMention> expectedMentions = Arrays.asList(line1entity1, line1entity2, line1entity3);
        List<TermRelationship> expectedRelationships = Arrays.asList(resolvedRel1, resolvedRel2);

        TermExtractionResult expected = new TermExtractionResult();
        expected.addAllTermMentions(expectedMentions);
        expected.addAllRelationships(expectedRelationships);

        TermExtractionResult result = instance.mapDocument(mappingReader, TEST_PROCESS_ID, "", visibility);
        assertEquals(expected, result);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMapDocument() throws Exception {
        TermMention line1entity1 = mock(TermMention.class, "l1t1");
        TermMention line1entity2 = mock(TermMention.class, "l1t2");
        TermMention line1entity3 = mock(TermMention.class, "l1t3");
        TermMention line2entity1 = mock(TermMention.class, "l2t1");
        TermMention line2entity3 = mock(TermMention.class, "l2t3");
        TermMention line3entity1 = mock(TermMention.class, "l3t1");
        TermMention line3entity2 = mock(TermMention.class, "l3t2");
        TermMention line3entity3 = mock(TermMention.class, "l3t3");
        String line1 = "line1";
        String line2 = "line2";
        String line3 = "line3";
        List<String> fields1 = Arrays.asList("foo", "bar", "fizz", "buzz");
        List<String> fields2 = Arrays.asList("this", "is", "a", "test");
        List<String> fields3 = Arrays.asList("the", "pen", "is", "blue");
        int lineOffset1 = 30;
        int lineOffset2 = 60;
        int lineOffset3 = 90;
        int line1Term1Offset = lineOffset1;
        int line1Term2Offset = line1Term1Offset + fields1.get(0).length() + 1;
        int line1Term3Offset = line1Term2Offset + fields1.get(1).length() + fields1.get(2).length() + 2;
        int line2Term1Offset = lineOffset2;
        int line2Term2Offset = line2Term1Offset + fields2.get(0).length() + 1;
        int line2Term3Offset = line2Term2Offset + fields2.get(1).length() + fields2.get(2).length() + 2;
        int line3Term1Offset = lineOffset3;
        int line3Term2Offset = line3Term1Offset + fields3.get(0).length() + 1;
        int line3Term3Offset = line3Term2Offset + fields3.get(1).length() + fields3.get(2).length() + 2;
        Map<String, TermMention> line1map = new HashMap<String, TermMention>();
        line1map.put(ENTITY1_ID, line1entity1);
        line1map.put(ENTITY2_ID, line1entity2);
        line1map.put(ENTITY3_ID, line1entity3);
        Map<String, TermMention> line2map = new HashMap<String, TermMention>();
        line2map.put(ENTITY1_ID, line2entity1);
        line2map.put(ENTITY3_ID, line2entity3);
        Map<String, TermMention> line3map = new HashMap<String, TermMention>();
        line3map.put(ENTITY1_ID, line3entity1);
        line3map.put(ENTITY2_ID, line3entity2);
        line3map.put(ENTITY3_ID, line3entity3);

        TermRelationship resolvedRel1 = new TermRelationship(line1entity1, line1entity2, LIVES_AT, visibility);
        TermRelationship resolvedRel2 = new TermRelationship(line1entity3, line1entity1, KNOWS, visibility);
        TermRelationship resolvedRel3 = new TermRelationship(line2entity3, line2entity1, KNOWS, visibility);
        TermRelationship resolvedRel4 = new TermRelationship(line3entity1, line3entity2, LIVES_AT, visibility);
        TermRelationship resolvedRel5 = new TermRelationship(line3entity3, line3entity1, KNOWS, visibility);

        when(rowIter.hasNext()).thenReturn(true, true, true, false);
        when(rowIter.next()).thenReturn(
                new Row(lineOffset1, fields1),
                new Row(lineOffset2, fields2),
                new Row(lineOffset3, fields3)
        );
        when(entity1.mapTerm(fields1, line1Term1Offset, TEST_PROCESS_ID, "", visibility)).thenReturn(line1entity1);
        when(entity2.mapTerm(fields1, line1Term2Offset, TEST_PROCESS_ID, "", visibility)).thenReturn(line1entity2);
        when(entity3.mapTerm(fields1, line1Term3Offset, TEST_PROCESS_ID, "", visibility)).thenReturn(line1entity3);
        when(entity1.mapTerm(fields2, line2Term1Offset, TEST_PROCESS_ID, "", visibility)).thenReturn(line2entity1);
        when(entity2.mapTerm(fields2, line2Term2Offset, TEST_PROCESS_ID, "", visibility)).thenReturn(null);
        when(entity3.mapTerm(fields2, line2Term3Offset, TEST_PROCESS_ID, "", visibility)).thenReturn(line2entity3);
        when(entity1.mapTerm(fields3, line3Term1Offset, TEST_PROCESS_ID, "", visibility)).thenReturn(line3entity1);
        when(entity2.mapTerm(fields3, line3Term2Offset, TEST_PROCESS_ID, "", visibility)).thenReturn(line3entity2);
        when(entity3.mapTerm(fields3, line3Term3Offset, TEST_PROCESS_ID, "", visibility)).thenReturn(line3entity3);
        when(rel1.createRelationship(line1map, fields1, visibility)).thenReturn(resolvedRel1);
        when(rel2.createRelationship(line1map, fields1, visibility)).thenReturn(resolvedRel2);
        when(rel2.createRelationship(line2map, fields2, visibility)).thenReturn(resolvedRel3);
        when(rel1.createRelationship(line3map, fields3, visibility)).thenReturn(resolvedRel4);
        when(rel2.createRelationship(line3map, fields3, visibility)).thenReturn(resolvedRel5);

        List<TermMention> expectedMentions = Arrays.asList(
                line1entity1, line1entity2, line1entity3,
                line2entity1, line2entity3,
                line3entity1, line3entity2, line3entity3
        );
        List<TermRelationship> expectedRelationships = Arrays.asList(
                resolvedRel1,
                resolvedRel2,
                resolvedRel3,
                resolvedRel4,
                resolvedRel5
        );
        TermExtractionResult expected = new TermExtractionResult();
        expected.addAllTermMentions(expectedMentions);
        expected.addAllRelationships(expectedRelationships);

        TermExtractionResult result = instance.mapDocument(mappingReader, TEST_PROCESS_ID, "", visibility);
        assertEquals(expected, result);
    }

    @SuppressWarnings("unchecked")
    private void doConstructorTest(final String testName, final String subject, final Class<? extends Throwable> expectedError) {
        doConstructorTest(testName, subject, entityMap, Collections.EMPTY_LIST, Collections.EMPTY_LIST, expectedError);
    }

    @SuppressWarnings("unchecked")
    private void doConstructorTest(final String testName, final Map<String, ColumnEntityMapping> entities,
                                   final Class<? extends Throwable> expectedError) {
        doConstructorTest(testName, TEST_SUBJECT, entities, Collections.EMPTY_LIST, Collections.EMPTY_LIST, expectedError);
    }

    private void doConstructorTest(final String testName, final String subject, final Map<String, ColumnEntityMapping> entities,
                                   final List<ColumnRelationshipMapping> relationships,
                                   final List<ColumnVertexRelationshipMapping> vertexRelationships, final Class<? extends Throwable> expectedError) {
        try {
            new TestImpl(subject, entities, relationships, vertexRelationships);
            fail(String.format("[%s] Expected %s.", testName, expectedError.getName()));
        } catch (Exception e) {
            assertTrue(String.format("[%s] expected %s, got %s.", testName, expectedError.getName(), e.getClass().getName()),
                    expectedError.isAssignableFrom(e.getClass()));
        }
    }

    private void doConstructorTest(final String testName, final String subject) {
        doConstructorTest(testName, subject, subject);
    }

    @SuppressWarnings("unchecked")
    private void doConstructorTest(final String testName, final String subject, final String expSubject) {
        doConstructorTest(testName, subject, entityMap, Collections.EMPTY_LIST,
                expSubject, entityMap, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
    }

    @SuppressWarnings("unchecked")
    private void doConstructorTest_Entities(final String testName, final Map<String, ColumnEntityMapping> entities) {
        doConstructorTest(testName, TEST_SUBJECT, entities, Collections.EMPTY_LIST,
                TEST_SUBJECT, entities, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
    }

    private void doConstructorTest_Rels(final String testName, final List<ColumnRelationshipMapping> rels, final List<ColumnVertexRelationshipMapping> vertexRels) {
        doConstructorTest_Rels(testName, rels, rels, vertexRels);
    }

    private void doConstructorTest_Rels(final String testName, final List<ColumnRelationshipMapping> rels,
                                        final List<ColumnRelationshipMapping> expRels,
                                        final List<ColumnVertexRelationshipMapping> expVertexRels) {
        doConstructorTest(testName, TEST_SUBJECT, entityMap, rels, TEST_SUBJECT, entityMap, expRels, expVertexRels);
    }

    private void doConstructorTest(final String testName, final String subject, final Map<String, ColumnEntityMapping> entities,
                                   final List<ColumnRelationshipMapping> relationships, final String expSubject,
                                   final Map<String, ColumnEntityMapping> expEntities, final List<ColumnRelationshipMapping> expRelationships,
                                   final List<ColumnVertexRelationshipMapping> expVertexRelationships) {
        AbstractColumnDocumentMapping mapping = new TestImpl(subject, entities, relationships, expVertexRelationships);
        assertEquals(testName, expSubject, mapping.getSubject());
        assertEquals(testName, expEntities, mapping.getEntities());
        assertEquals(testName, expRelationships, mapping.getRelationships());
        assertEquals(testName, expVertexRelationships, mapping.getVertexRelationships());
    }

    private static interface Delegate {
        Iterable<Row> getRows(Reader reader);

        void ingestDocument(InputStream inputDoc, Writer outputDoc) throws IOException;
    }

    private class TestImpl extends AbstractColumnDocumentMapping {

        public TestImpl(String subject, Map<String, ColumnEntityMapping> entities, List<ColumnRelationshipMapping> relationships, List<ColumnVertexRelationshipMapping> vertexRelationships) {
            super(subject, entities, relationships, vertexRelationships);
        }

        @Override
        protected Iterable<Row> getRows(Reader reader) {
            return delegate.getRows(reader);
        }

        @Override
        public void ingestDocument(InputStream inputDoc, Writer outputDoc) throws IOException {
            delegate.ingestDocument(inputDoc, outputDoc);
        }
    }
}
