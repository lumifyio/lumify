package io.lumify.phoneNumber;

import com.google.common.base.Charsets;
import com.google.inject.Injector;
import io.lumify.core.config.HashMapConfigurationLoader;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import io.lumify.core.ingest.graphProperty.TermMentionFilter;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.termMention.TermMentionRepository;
import io.lumify.core.security.DirectVisibilityTranslator;
import io.lumify.core.security.VisibilityTranslator;
import io.lumify.core.user.User;
import io.lumify.web.clientapi.model.VisibilityJson;
import org.apache.hadoop.fs.FileSystem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.securegraph.*;
import org.securegraph.inmemory.InMemoryAuthorizations;
import org.securegraph.inmemory.InMemoryGraph;
import org.securegraph.property.StreamingPropertyValue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.securegraph.util.IterableUtils.toList;

@RunWith(MockitoJUnitRunner.class)
public class PhoneNumberGraphPropertyWorkerTest {
    private static final String PHONE_TEXT = "This terrorist's phone number is 410-678-2230, and his best buddy's phone number is +44 (0)207 437 0478";
    private static final String PHONE_NEW_LINES = "This terrorist's phone\n number is 410-678-2230, and his best buddy's phone number\n is +44 (0)207 437 0478";
    private static final String PHONE_MISSING = "This is a sentence without any phone numbers in it.";

    @Mock
    private User user;

    @Mock
    private OntologyRepository ontologyRepository;

    private PhoneNumberGraphPropertyWorker extractor;
    private InMemoryAuthorizations authorizations;
    private InMemoryGraph graph;
    private Visibility visibility;
    private VisibilityTranslator visibilityTranslator = new DirectVisibilityTranslator();
    private VisibilityJson visibilityJson;

    @Before
    public void setUp() throws Exception {

        Map config = new HashMap();
        config.put("ontology.iri.concept.person", "http://lumify.io/test#person");
        config.put("ontology.iri.concept.location", "http://lumify.io/test#location");
        config.put("ontology.iri.concept.organization", "http://lumify.io/test#organization");
        config.put("ontology.iri.relationship.artifactHasEntity", "http://lumify.io/test#artifactHasEntity");
        config.put("ontology.iri.concept.phoneNumber", "http://lumify.io/test#phoneNumber");
        io.lumify.core.config.Configuration configuration = new HashMapConfigurationLoader(config).createConfiguration();

        when(ontologyRepository.getRequiredConceptIRIByIntent("phoneNumber")).thenReturn("http://lumify.io/test#phoneNumber");

        extractor = new PhoneNumberGraphPropertyWorker();
        extractor.setConfiguration(configuration);
        extractor.setVisibilityTranslator(visibilityTranslator);
        extractor.setOntologyRepository(ontologyRepository);

        FileSystem hdfsFileSystem = null;
        authorizations = new InMemoryAuthorizations(TermMentionRepository.VISIBILITY);
        Injector injector = null;
        List<TermMentionFilter> termMentionFilters = new ArrayList<TermMentionFilter>();
        GraphPropertyWorkerPrepareData workerPrepareData = new GraphPropertyWorkerPrepareData(config, termMentionFilters, hdfsFileSystem, user, authorizations, injector);
        graph = InMemoryGraph.create();
        visibility = new Visibility("");
        visibilityJson = new VisibilityJson();

        extractor.setGraph(graph);

        extractor.prepare(workerPrepareData);
    }

    @Test
    public void testPhoneNumberExtraction() throws Exception {
        InputStream in = asStream(PHONE_TEXT);
        VertexBuilder vertexBuilder = graph.prepareVertex("v1", visibility);
        StreamingPropertyValue textPropertyValue = new StreamingPropertyValue(in, String.class);
        LumifyProperties.TEXT.setProperty(vertexBuilder, textPropertyValue, visibility);
        LumifyProperties.VISIBILITY_JSON.setProperty(vertexBuilder, visibilityJson, visibility);
        Vertex vertex = vertexBuilder.save(authorizations);

        Property property = vertex.getProperty(LumifyProperties.TEXT.getPropertyName());
        GraphPropertyWorkData workData = new GraphPropertyWorkData(visibilityTranslator, vertex, property, null, null);
        in = asStream(PHONE_TEXT);
        extractor.execute(in, workData);

        List<Vertex> termMentions = toList(vertex.getVertices(Direction.OUT, LumifyProperties.TERM_MENTION_LABEL_HAS_TERM_MENTION, authorizations));

        assertEquals("Incorrect number of phone numbers extracted", 2, termMentions.size());

        boolean foundFirst = false;
        boolean foundSecond = false;
        for (Vertex term : termMentions) {
            String title = LumifyProperties.TERM_MENTION_TITLE.getPropertyValue(term);
            if (title.equals("+14106782230")) {
                foundFirst = true;
                assertEquals(33, LumifyProperties.TERM_MENTION_START_OFFSET.getPropertyValue(term, 0));
                assertEquals(45, LumifyProperties.TERM_MENTION_END_OFFSET.getPropertyValue(term, 0));
            } else if (title.equals("+442074370478")) {
                foundSecond = true;
                assertEquals(84, LumifyProperties.TERM_MENTION_START_OFFSET.getPropertyValue(term, 0));
                assertEquals(103, LumifyProperties.TERM_MENTION_END_OFFSET.getPropertyValue(term, 0));
            }
        }
        assertTrue("+14106782230 not found", foundFirst);
        assertTrue("+442074370478 not found", foundSecond);
    }

    @Test
    public void testPhoneNumberExtractionWithNewlines() throws Exception {
        InputStream in = asStream(PHONE_NEW_LINES);
        VertexBuilder vertexBuilder = graph.prepareVertex("v1", visibility);
        StreamingPropertyValue textPropertyValue = new StreamingPropertyValue(in, String.class);
        LumifyProperties.TEXT.setProperty(vertexBuilder, textPropertyValue, visibility);
        LumifyProperties.VISIBILITY_JSON.setProperty(vertexBuilder, visibilityJson, visibility);
        Vertex vertex = vertexBuilder.save(authorizations);

        Property property = vertex.getProperty(LumifyProperties.TEXT.getPropertyName());
        GraphPropertyWorkData workData = new GraphPropertyWorkData(visibilityTranslator, vertex, property, null, null);
        in = asStream(PHONE_NEW_LINES);
        extractor.execute(in, workData);

        List<Vertex> termMentions = toList(vertex.getVertices(Direction.OUT, LumifyProperties.TERM_MENTION_LABEL_HAS_TERM_MENTION, authorizations));

        assertEquals("Incorrect number of phone numbers extracted", 2, termMentions.size());

        boolean foundFirst = false;
        boolean foundSecond = false;
        for (Vertex term : termMentions) {
            String title = LumifyProperties.TERM_MENTION_TITLE.getPropertyValue(term);
            if (title.equals("+14106782230")) {
                foundFirst = true;
                assertEquals(34, LumifyProperties.TERM_MENTION_START_OFFSET.getPropertyValue(term, 0));
                assertEquals(46, LumifyProperties.TERM_MENTION_END_OFFSET.getPropertyValue(term, 0));
            } else if (title.equals("+442074370478")) {
                foundSecond = true;
                assertEquals(86, LumifyProperties.TERM_MENTION_START_OFFSET.getPropertyValue(term, 0));
                assertEquals(105, LumifyProperties.TERM_MENTION_END_OFFSET.getPropertyValue(term, 0));
            }
        }
        assertTrue("+14106782230 not found", foundFirst);
        assertTrue("+442074370478 not found", foundSecond);
    }

    @Test
    public void testNegativePhoneNumberExtraction() throws Exception {
        InputStream in = asStream(PHONE_MISSING);
        VertexBuilder vertexBuilder = graph.prepareVertex("v1", visibility);
        StreamingPropertyValue textPropertyValue = new StreamingPropertyValue(in, String.class);
        LumifyProperties.TEXT.setProperty(vertexBuilder, textPropertyValue, visibility);
        Vertex vertex = vertexBuilder.save(authorizations);

        Property property = vertex.getProperty(LumifyProperties.TEXT.getPropertyName());
        GraphPropertyWorkData workData = new GraphPropertyWorkData(visibilityTranslator, vertex, property, null, null);
        in = asStream(PHONE_MISSING);
        extractor.execute(in, workData);

        List<Vertex> termMentions = toList(vertex.getVertices(Direction.OUT, LumifyProperties.TERM_MENTION_LABEL_HAS_TERM_MENTION, authorizations));

        assertTrue("Phone number extracted when there were no phone numbers", termMentions.isEmpty());
    }

    private InputStream asStream(final String text) {
        return new ByteArrayInputStream(text.getBytes(Charsets.UTF_8));
    }
}
