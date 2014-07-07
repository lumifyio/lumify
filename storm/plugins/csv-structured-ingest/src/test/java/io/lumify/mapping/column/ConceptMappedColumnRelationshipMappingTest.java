package io.lumify.mapping.column;

import io.lumify.core.ingest.term.extraction.TermMention;
import io.lumify.core.model.ontology.Concept;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.mapping.column.ConceptMappedColumnRelationshipMapping.ConceptMapping;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConceptMappedColumnRelationshipMappingTest {
    private static final String TEST_SRC_ID = "testSrc";
    private static final String TEST_TGT_ID = "testTgt";
    private static final String EMPTY = "";
    private static final String WHITESPACE = "\n \t\t \n";
    private static final String FOO = "foo";
    private static final String FOO_BAR_LABEL = "fooThenBar";
    private static final String BAR = "bar";
    private static final String ROOT = "root";
    private static final String LOCATION = "location";
    private static final String CITY = "city";
    private static final String CAPITAL = "capital";
    private static final String PERSON = "person";
    private static final String TERRORIST = "terrorist";

    private static final String TERRORIST_ATTACKED_LOCATION = "terroristAttackedLocation";
    private static final String CITY_HOUSES_PERSON = "cityHousesPerson";
    private static final String PERSON_KNOWS = "personKnows";
    private static final String FOO_RELATED_TO = "fooRelatedTo";
    private static final String RELATED_TO_BAR = "relatedToBar";
    private static final String FOUND_AT_LOCATION = "foundAtLocation";

    private static final ConceptMapping FOO_SOURCE_MAPPING = new ConceptMapping(FOO, null, FOO_RELATED_TO);
    private static final ConceptMapping PERSON_SOURCE_MAPPING = new ConceptMapping(PERSON, null, PERSON_KNOWS);
    private static final ConceptMapping BAR_TARGET_MAPPING = new ConceptMapping(null, BAR, RELATED_TO_BAR);
    private static final ConceptMapping LOCATION_TARGET_MAPPING = new ConceptMapping(null, LOCATION, FOUND_AT_LOCATION);
    private static final ConceptMapping FOO_BAR_MAPPING = new ConceptMapping(FOO, BAR, FOO_BAR_LABEL);
    private static final ConceptMapping TERRORIST_LOCATION_MAPPING = new ConceptMapping(TERRORIST, LOCATION, TERRORIST_ATTACKED_LOCATION);
    private static final ConceptMapping CITY_PERSON_MAPPING = new ConceptMapping(CITY, PERSON, CITY_HOUSES_PERSON);

    @Mock
    private OntologyRepository ontologyRepo;
    @Mock
    private Concept rootConcept;
    @Mock
    private Concept fooConcept;
    @Mock
    private Concept barConcept;
    @Mock
    private Concept locationConcept;
    @Mock
    private Concept cityConcept;
    @Mock
    private Concept capitalConcept;
    @Mock
    private Concept personConcept;
    @Mock
    private Concept terroristConcept;
    @Mock
    private TermMention srcMention;
    @Mock
    private TermMention tgtMention;

    @Before
    public void setup() {
        // configure Concept ontology
        // - root
        //   - foo
        //   - bar
        //   - location
        //     - city
        //       - capital
        //   - person
        //     - terrorist
        Map<String, Concept> cMap = new HashMap<String, Concept>();
        cMap.put(ROOT, rootConcept);
        cMap.put(FOO, fooConcept);
        cMap.put(BAR, barConcept);
        cMap.put(LOCATION, locationConcept);
        cMap.put(CITY, cityConcept);
        cMap.put(CAPITAL, capitalConcept);
        cMap.put(PERSON, personConcept);
        cMap.put(TERRORIST, terroristConcept);
        Map<Concept, Concept> pMap = new HashMap<Concept, Concept>();
        pMap.put(rootConcept, null);
        pMap.put(fooConcept, rootConcept);
        pMap.put(barConcept, rootConcept);
        pMap.put(locationConcept, rootConcept);
        pMap.put(cityConcept, locationConcept);
        pMap.put(capitalConcept, cityConcept);
        pMap.put(personConcept, rootConcept);
        pMap.put(terroristConcept, personConcept);
        for (Map.Entry<String, Concept> entry : cMap.entrySet()) {
            when(ontologyRepo.getConceptByIRI(entry.getKey())).thenReturn(entry.getValue());
            when(entry.getValue().getTitle()).thenReturn(entry.getKey());
            when(entry.getValue().getTitle()).thenReturn(entry.getKey());
            when(ontologyRepo.getParentConcept(entry.getValue())).thenReturn(pMap.get(entry.getValue()));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testIllegalConstruction() {
        doTestConstructor("null mappings", null, NullPointerException.class);
        doTestConstructor("empty mappings", Collections.EMPTY_LIST, IllegalArgumentException.class);
    }

    @Test
    public void testLegalConstruction() {
        ConceptMapping[] expected = new ConceptMapping[]{
                FOO_SOURCE_MAPPING,
                FOO_BAR_MAPPING,
                BAR_TARGET_MAPPING,
                TERRORIST_LOCATION_MAPPING,
                CITY_PERSON_MAPPING
        };
        assertEquals(Arrays.asList(expected), createInstance(expected).getLabelMappings());
    }

    @Test
    public void testIllegalMappingConstruction() {
        doTestMappingConstructor_SourceTarget("null source, null target", null, null, IllegalArgumentException.class);
        doTestMappingConstructor_SourceTarget("null source, empty target", null, EMPTY, IllegalArgumentException.class);
        doTestMappingConstructor_SourceTarget("null source, whitespace target", null, WHITESPACE, IllegalArgumentException.class);
        doTestMappingConstructor_SourceTarget("empty source, null target", EMPTY, null, IllegalArgumentException.class);
        doTestMappingConstructor_SourceTarget("empty source, empty target", EMPTY, EMPTY, IllegalArgumentException.class);
        doTestMappingConstructor_SourceTarget("empty source, whitespace target", EMPTY, WHITESPACE, IllegalArgumentException.class);
        doTestMappingConstructor_SourceTarget("whitespace source, null target", WHITESPACE, null, IllegalArgumentException.class);
        doTestMappingConstructor_SourceTarget("whitespace source, empty target", WHITESPACE, EMPTY, IllegalArgumentException.class);
        doTestMappingConstructor_SourceTarget("whitespace source, whitespace target", WHITESPACE, WHITESPACE,
                IllegalArgumentException.class);
        doTestMappingConstructor_Label("null label", null, NullPointerException.class);
        doTestMappingConstructor_Label("empty label", EMPTY, IllegalArgumentException.class);
        doTestMappingConstructor_Label("whitespace label", WHITESPACE, IllegalArgumentException.class);
    }

    @Test
    public void testLegalMappingConstruction() {
        doTestMappingConstructor_SourceTarget("trimmed source, null target", FOO, null, FOO, null);
        doTestMappingConstructor_SourceTarget("untrimmed source, empty target", "\t  " + FOO + "\t\t  \n", EMPTY, FOO, null);
        doTestMappingConstructor_SourceTarget("trimmed source, whitespace target", FOO, WHITESPACE, FOO, null);
        doTestMappingConstructor_SourceTarget("null source, trimmed target", null, BAR, null, BAR);
        doTestMappingConstructor_SourceTarget("empty source, untrimmed target", EMPTY, "\t  " + BAR + "\t\t  \n", null, BAR);
        doTestMappingConstructor_SourceTarget("whitespace source, trimmed target", WHITESPACE, BAR, null, BAR);
        doTestMappingConstructor_SourceTarget("trimmed source, trimmed target", FOO, BAR, FOO, BAR);
        doTestMappingConstructor_SourceTarget("untrimmed source, untrimmed target", "  " + FOO + "\t\n", "\t  " + BAR + "\n", FOO, BAR);
        doTestMappingConstructor_Label("trimmed label", FOO_BAR_LABEL, FOO_BAR_LABEL);
        doTestMappingConstructor_Label("untrimmed label", "\t  " + FOO_BAR_LABEL + "\t\t  \n", FOO_BAR_LABEL);
    }

    @Test
    public void testSourceOnlyMapping() {
        ConceptMappedColumnRelationshipMapping instance = createInstance(FOO_SOURCE_MAPPING, PERSON_SOURCE_MAPPING);
        doGetLabelTest("null source mention", instance, (TermMention) null, null, null);
        doGetLabelTest("null source URI", instance, (String) null, null, null);
        doGetLabelTest("exact source", instance, FOO, null, FOO_RELATED_TO);
        doGetLabelTest("descendant source", instance, TERRORIST, null, PERSON_KNOWS);
        doGetLabelTest("unconfigured source", instance, CAPITAL, null, null);
    }

    @Test
    public void testTargetOnlyMapping() {
        ConceptMappedColumnRelationshipMapping instance = createInstance(BAR_TARGET_MAPPING, LOCATION_TARGET_MAPPING);
        doGetLabelTest("null target mention", instance, null, (TermMention) null, null);
        doGetLabelTest("null target URI", instance, null, (String) null, null);
        doGetLabelTest("exact target", instance, null, BAR, RELATED_TO_BAR);
        doGetLabelTest("descendant target", instance, null, CAPITAL, FOUND_AT_LOCATION);
        doGetLabelTest("unconfigured target", instance, null, TERRORIST, null);
    }

    @Test
    public void testSourceTargetMapping() {
        ConceptMappedColumnRelationshipMapping instance = createInstance(
                FOO_SOURCE_MAPPING,
                BAR_TARGET_MAPPING,
                PERSON_SOURCE_MAPPING,
                LOCATION_TARGET_MAPPING,
                FOO_BAR_MAPPING,
                TERRORIST_LOCATION_MAPPING,
                CITY_PERSON_MAPPING
        );

        doGetLabelTest("null source & target mentions", instance, (TermMention) null, null, null);
        doGetLabelTest("null source & target URIs", instance, (String) null, null, null);
        doGetLabelTest("exact source & target match", instance, FOO, BAR, FOO_BAR_LABEL);
        doGetLabelTest("default source match", instance, FOO, CITY, FOO_RELATED_TO);
        doGetLabelTest("default parent source match", instance, TERRORIST, BAR, PERSON_KNOWS);
        doGetLabelTest("default target match", instance, LOCATION, BAR, RELATED_TO_BAR);
        doGetLabelTest("default parent target match", instance, CITY, CAPITAL, FOUND_AT_LOCATION);
        doGetLabelTest("exact source, parent target match", instance, TERRORIST, CAPITAL, TERRORIST_ATTACKED_LOCATION);
        doGetLabelTest("parent source, exact target match", instance, CAPITAL, PERSON, CITY_HOUSES_PERSON);
        doGetLabelTest("parent source, parent target match", instance, CAPITAL, TERRORIST, CITY_HOUSES_PERSON);
        doGetLabelTest("no matching pairs", instance, BAR, FOO, null);
    }

    private void doGetLabelTest(final String testName, final ConceptMappedColumnRelationshipMapping instance,
                                final String sourceConcept, final String targetConcept, final String expLabel) {
        when(srcMention.getOntologyClassUri()).thenReturn(sourceConcept);
        when(tgtMention.getOntologyClassUri()).thenReturn(targetConcept);
        doGetLabelTest(testName, instance, srcMention, tgtMention, expLabel);
    }

    private void doGetLabelTest(final String testName, final ConceptMappedColumnRelationshipMapping instance,
                                final TermMention source, final TermMention target, final String expLabel) {
        String label = instance.getLabel(source, target, null);
        assertEquals(String.format("[%s]: ", testName), expLabel, label);
    }

    private ConceptMappedColumnRelationshipMapping createInstance(final ConceptMapping... mappings) {
        ConceptMappedColumnRelationshipMapping instance =
                new ConceptMappedColumnRelationshipMapping(TEST_SRC_ID, TEST_TGT_ID, Arrays.asList(mappings));
        instance.setOntologyRepository(ontologyRepo);
        return instance;
    }

    private void doTestConstructor(final String testName, final List<ConceptMapping> lblMap, final Class<? extends Throwable> expError) {
        try {
            new ConceptMappedColumnRelationshipMapping(TEST_SRC_ID, TEST_TGT_ID, lblMap);
            fail(String.format("[%s]: Expected %s", testName, expError.getName()));
        } catch (Exception e) {
            assertTrue(String.format("[%s]: Expected %s, got %s", testName, expError.getName(), e.getClass().getName()),
                    expError.isAssignableFrom(e.getClass()));
        }
    }

    private void doTestMappingConstructor_SourceTarget(final String testName, final String source, final String target,
                                                       final Class<? extends Throwable> expError) {
        doTestMappingConstructor(testName, source, target, FOO_BAR_LABEL, expError);
    }

    private void doTestMappingConstructor_Label(final String testName, final String label, final Class<? extends Throwable> expError) {
        doTestMappingConstructor(testName, FOO, BAR, label, expError);
    }

    private void doTestMappingConstructor(final String testName, final String source, final String target, final String label,
                                          final Class<? extends Throwable> expError) {
        try {
            new ConceptMapping(source, target, label);
            fail(String.format("[%s]: Expected %s", testName, expError.getName()));
        } catch (Exception e) {
            assertTrue(String.format("[%s]: Expected %s, got %s", testName, expError.getName(), e.getClass().getName()),
                    expError.isAssignableFrom(e.getClass()));
        }
    }

    private void doTestMappingConstructor_SourceTarget(final String testName, final String source, final String target,
                                                       final String expSource, final String expTarget) {
        doTestMappingConstructor(testName, source, target, FOO_BAR_LABEL, expSource, expTarget, FOO_BAR_LABEL);
    }

    private void doTestMappingConstructor_Label(final String testName, final String label, final String expLabel) {
        doTestMappingConstructor(testName, FOO, BAR, label, FOO, BAR, expLabel);
    }

    private void doTestMappingConstructor(final String testName, final String source, final String target, final String label,
                                          final String expSource, final String expTarget, final String expLabel) {
        ConceptMapping mapping = new ConceptMapping(source, target, label);
        String msg = String.format("[%s]: ", testName);
        assertEquals(msg, expSource, mapping.getSourceConcept());
        assertEquals(msg, expTarget, mapping.getTargetConcept());
        assertEquals(msg, expLabel, mapping.getRelationshipLabel());
    }
}
