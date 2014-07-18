package io.lumify.opennlpme;

import com.google.inject.Injector;
import io.lumify.core.config.HashMapConfigurationLoader;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import io.lumify.core.ingest.graphProperty.TermMentionFilter;
import io.lumify.core.ingest.term.extraction.TermMention;
import io.lumify.core.user.User;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.mapreduce.Mapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.securegraph.Vertex;
import org.securegraph.Visibility;
import org.securegraph.inmemory.InMemoryAuthorizations;
import org.securegraph.inmemory.InMemoryGraph;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.securegraph.util.IterableUtils.toList;

@RunWith(MockitoJUnitRunner.class)
public class OpenNLPMaximumEntropyExtractorGraphPropertyWorkerTest {
    private static final String RESOURCE_CONFIG_DIR = "/fs/conf/opennlp";

    private OpenNLPMaximumEntropyExtractorGraphPropertyWorker extractor;

    @Mock
    private Mapper.Context context;

    @Mock
    private User user;
    private String text = "This is a sentence, written by Bob Robertson, who currently makes 2 million "
            + "a year. If by 1:30, you don't know what you are doing, you should go watch CNN and see "
            + "what the latest is on the Benghazi nonsense. I'm 47% sure that this test will pass, but will it?";

    List<TermMention> termMentions;

    private InMemoryAuthorizations authorizations;
    private InMemoryGraph graph;

    @Before
    public void setUp() throws Exception {
        graph = new InMemoryGraph();

        Map config = new HashMap();
        config.put(io.lumify.core.config.Configuration.ONTOLOGY_IRI_ARTIFACT_HAS_ENTITY, "http://lumify.io/test#artifactHasEntity");
        io.lumify.core.config.Configuration configuration = new HashMapConfigurationLoader(config).createConfiguration();

        extractor = new OpenNLPMaximumEntropyExtractorGraphPropertyWorker() {
            @Override
            protected List<TermMentionWithGraphVertex> saveTermMentions(Vertex artifactGraphVertex, Iterable<TermMention> termMentions) {
                OpenNLPMaximumEntropyExtractorGraphPropertyWorkerTest.this.termMentions = toList(termMentions);
                return null;
            }
        };
        extractor.setConfiguration(configuration);
        extractor.setGraph(graph);

        Map<String, String> stormConf = new HashMap<String, String>();
        stormConf.put(io.lumify.core.config.Configuration.ONTOLOGY_IRI_LOCATION, "http://lumify.io/test#location");
        stormConf.put(io.lumify.core.config.Configuration.ONTOLOGY_IRI_ORGANIZATION, "http://lumify.io/test#organization");
        stormConf.put(io.lumify.core.config.Configuration.ONTOLOGY_IRI_PERSON, "http://lumify.io/test#person");
        stormConf.put(OpenNLPMaximumEntropyExtractorGraphPropertyWorker.PATH_PREFIX_CONFIG, "file:///" + getClass().getResource(RESOURCE_CONFIG_DIR).getFile());

        FileSystem hdfsFileSystem = FileSystem.get(new Configuration());
        authorizations = new InMemoryAuthorizations();
        Injector injector = null;
        List<TermMentionFilter> termMentionFilters = new ArrayList<TermMentionFilter>();
        GraphPropertyWorkerPrepareData workerPrepareData = new GraphPropertyWorkerPrepareData(stormConf, termMentionFilters, hdfsFileSystem, user, authorizations, injector);
        extractor.prepare(workerPrepareData);
    }

    @Test
    public void testEntityExtraction() throws Exception {
        Vertex vertex = graph.prepareVertex("v1", new Visibility(""))
                .setProperty("text", "none", new Visibility(""))
                .save(new InMemoryAuthorizations());

        GraphPropertyWorkData workData = new GraphPropertyWorkData(vertex, vertex.getProperty("text"));
        extractor.execute(new ByteArrayInputStream(text.getBytes()), workData);
        HashMap<String, TermMention> extractedTerms = new HashMap<String, TermMention>();
        for (TermMention term : termMentions) {
            extractedTerms.put(term.getSign() + "-" + term.getOntologyClassUri(), term);
        }
        assertTrue("A person wasn't found", extractedTerms.containsKey("Bob Robertson-http://lumify.io/test#person"));
        TermMention bobRobertsonMentions = extractedTerms.get("Bob Robertson-http://lumify.io/test#person");
        assertEquals(31, bobRobertsonMentions.getStart());
        assertEquals(44, bobRobertsonMentions.getEnd());


        assertTrue("A location wasn't found", extractedTerms.containsKey("Benghazi-http://lumify.io/test#location"));
        TermMention benghaziMentions = extractedTerms.get("Benghazi-http://lumify.io/test#location");
        assertEquals(189, benghaziMentions.getStart());
        assertEquals(197, benghaziMentions.getEnd());

        assertTrue("An organization wasn't found", extractedTerms.containsKey("CNN-http://lumify.io/test#organization"));
        TermMention cnnMentions = extractedTerms.get("CNN-http://lumify.io/test#organization");
        assertEquals(151, cnnMentions.getStart());
        assertEquals(154, cnnMentions.getEnd());
    }
}
