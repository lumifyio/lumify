package io.lumify.phoneNumber;

import com.google.common.base.Charsets;
import com.google.inject.Injector;
import io.lumify.core.config.HashMapConfigurationLoader;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import io.lumify.core.ingest.graphProperty.TermMentionFilter;
import io.lumify.core.ingest.term.extraction.TermMention;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.user.User;
import org.apache.hadoop.fs.FileSystem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.securegraph.Property;
import org.securegraph.Vertex;
import org.securegraph.VertexBuilder;
import org.securegraph.Visibility;
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
import static org.securegraph.util.IterableUtils.toList;

@RunWith(MockitoJUnitRunner.class)
public class PhoneNumberGraphPropertyWorkerTest {
    private static final String PHONE_TEXT = "This terrorist's phone number is 410-678-2230, and his best buddy's phone number is +44 (0)207 437 0478";
    private static final String PHONE_NEW_LINES = "This terrorist's phone\n number is 410-678-2230, and his best buddy's phone number\n is +44 (0)207 437 0478";
    private static final String PHONE_MISSING = "This is a sentence without any phone numbers in it.";

    @Mock
    private User user;

    private PhoneNumberGraphPropertyWorker extractor;
    private InMemoryAuthorizations authorizations;
    private InMemoryGraph graph;
    private Visibility visibility;
    private List<TermMention> termMentions;


    @Before
    public void setUp() throws Exception {

        Map config = new HashMap();
        config.put(io.lumify.core.config.Configuration.ONTOLOGY_IRI_ARTIFACT_HAS_ENTITY, "http://lumify.io/test#artifactHasEntity");
        config.put(PhoneNumberGraphPropertyWorker.CONFIG_PHONE_NUMBER_IRI, "http://lumify.io/test#phoneNumber");
        io.lumify.core.config.Configuration configuration = new HashMapConfigurationLoader(config).createConfiguration();;

        extractor = new PhoneNumberGraphPropertyWorker() {
            @Override
            protected List<TermMentionWithGraphVertex> saveTermMentions(Vertex artifactGraphVertex, Iterable<TermMention> termMentions) {
                PhoneNumberGraphPropertyWorkerTest.this.termMentions = toList(termMentions);
                return null;
            }
        };
        extractor.setConfiguration(configuration);

        FileSystem hdfsFileSystem = null;
        authorizations = new InMemoryAuthorizations();
        Injector injector = null;
        List<TermMentionFilter> termMentionFilters = new ArrayList<TermMentionFilter>();
        GraphPropertyWorkerPrepareData workerPrepareData = new GraphPropertyWorkerPrepareData(config, termMentionFilters, hdfsFileSystem, user, authorizations, injector);
        graph = new InMemoryGraph();
        visibility = new Visibility("");
        extractor.setGraph(graph);

        extractor.prepare(workerPrepareData);
    }

    @Test
    public void testPhoneNumberExtraction() throws Exception {
        InputStream in = asStream(PHONE_TEXT);
        VertexBuilder vertexBuilder = graph.prepareVertex("v1", visibility);
        StreamingPropertyValue textPropertyValue = new StreamingPropertyValue(in, String.class);
        LumifyProperties.TEXT.setProperty(vertexBuilder, textPropertyValue, visibility);
        Vertex vertex = vertexBuilder.save(authorizations);

        Property property = vertex.getProperty(LumifyProperties.TEXT.getPropertyName());
        GraphPropertyWorkData workData = new GraphPropertyWorkData(vertex, property);
        in = asStream(PHONE_TEXT);
        extractor.execute(in, workData);

        assertEquals("Incorrect number of phone numbers extracted", 2, termMentions.size());
        TermMention firstTerm = termMentions.get(0);
        assertEquals("First phone number not correctly extracted", "+14106782230", firstTerm.getSign());
        assertEquals(33, firstTerm.getStart());
        assertEquals(45, firstTerm.getEnd());

        TermMention secondTerm = termMentions.get(1);
        assertEquals("Second phone number not correctly extracted", "+442074370478", secondTerm.getSign());
        assertEquals(84, secondTerm.getStart());
        assertEquals(103, secondTerm.getEnd());
    }

    @Test
    public void testPhoneNumberExtractionWithNewlines() throws Exception {
        InputStream in = asStream(PHONE_NEW_LINES);
        VertexBuilder vertexBuilder = graph.prepareVertex("v1", visibility);
        StreamingPropertyValue textPropertyValue = new StreamingPropertyValue(in, String.class);
        LumifyProperties.TEXT.setProperty(vertexBuilder, textPropertyValue, visibility);
        Vertex vertex = vertexBuilder.save(authorizations);

        Property property = vertex.getProperty(LumifyProperties.TEXT.getPropertyName());
        GraphPropertyWorkData workData = new GraphPropertyWorkData(vertex, property);
        in = asStream(PHONE_NEW_LINES);
        extractor.execute(in, workData);

        assertEquals("Incorrect number of phone numbers extracted", 2, termMentions.size());
        TermMention firstTerm = termMentions.get(0);
        assertEquals("First phone number not correctly extracted", "+14106782230", firstTerm.getSign());
        assertEquals(34, firstTerm.getStart());
        assertEquals(46, firstTerm.getEnd());

        TermMention secondTerm = termMentions.get(1);
        assertEquals("Second phone number not correctly extracted", "+442074370478", secondTerm.getSign());
        assertEquals(86, secondTerm.getStart());
        assertEquals(105, secondTerm.getEnd());
    }

    @Test
    public void testNegativePhoneNumberExtraction() throws Exception {
        InputStream in = asStream(PHONE_MISSING);
        VertexBuilder vertexBuilder = graph.prepareVertex("v1", visibility);
        StreamingPropertyValue textPropertyValue = new StreamingPropertyValue(in, String.class);
        LumifyProperties.TEXT.setProperty(vertexBuilder, textPropertyValue, visibility);
        Vertex vertex = vertexBuilder.save(authorizations);

        Property property = vertex.getProperty(LumifyProperties.TEXT.getPropertyName());
        GraphPropertyWorkData workData = new GraphPropertyWorkData(vertex, property);
        in = asStream(PHONE_MISSING);
        extractor.execute(in, workData);

        assertTrue("Phone number extracted when there were no phone numbers", termMentions.isEmpty());
    }

    private InputStream asStream(final String text) {
        return new ByteArrayInputStream(text.getBytes(Charsets.UTF_8));
    }
}
