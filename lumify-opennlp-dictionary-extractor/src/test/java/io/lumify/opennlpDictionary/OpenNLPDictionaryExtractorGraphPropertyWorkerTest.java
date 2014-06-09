package io.lumify.opennlpDictionary;

import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import io.lumify.core.ingest.graphProperty.TermMentionFilter;
import io.lumify.core.ingest.term.extraction.TermMention;
import io.lumify.core.user.User;
import io.lumify.opennlpDictionary.model.DictionaryEntryRepository;
import org.securegraph.Vertex;
import org.securegraph.Visibility;
import org.securegraph.inmemory.InMemoryAuthorizations;
import org.securegraph.inmemory.InMemoryGraph;
import com.google.inject.Injector;
import opennlp.tools.dictionary.Dictionary;
import opennlp.tools.namefind.DictionaryNameFinder;
import opennlp.tools.namefind.TokenNameFinder;
import opennlp.tools.util.StringList;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.securegraph.util.IterableUtils.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class OpenNLPDictionaryExtractorGraphPropertyWorkerTest {
    private static final String RESOURCE_CONFIG_DIR = "/fs/conf/opennlp";

    private OpenNLPDictionaryExtractorGraphPropertyWorker extractor;

    @Mock
    private User user;

    private String text = "This is a sentence that is going to tell you about a guy named "
            + "Bob Robertson who lives in Boston, MA and works for a company called Altamira Corporation";

    private InMemoryAuthorizations authorizations;

    @Mock
    private DictionaryEntryRepository dictionaryEntryRepository;

    List<TermMention> termMentions;
    private InMemoryGraph graph;

    @Before
    public void setUp() throws Exception {
        final List<TokenNameFinder> finders = loadFinders();

        Map config = new HashMap();
        config.put(io.lumify.core.config.Configuration.ONTOLOGY_IRI_ARTIFACT_HAS_ENTITY, "http://lumify.io/test#artifactHasEntity");
        io.lumify.core.config.Configuration configuration = new io.lumify.core.config.Configuration(config);

        graph = new InMemoryGraph();

        extractor = new OpenNLPDictionaryExtractorGraphPropertyWorker() {
            @Override
            protected List<TokenNameFinder> loadFinders() throws IOException {
                return finders;
            }

            @Override
            protected List<TermMentionWithGraphVertex> saveTermMentions(Vertex artifactGraphVertex, Iterable<TermMention> termMentions) {
                OpenNLPDictionaryExtractorGraphPropertyWorkerTest.this.termMentions = toList(termMentions);
                return null;
            }
        };
        extractor.setConfiguration(configuration);
        extractor.setDictionaryEntryRepository(dictionaryEntryRepository);
        extractor.setGraph(graph);

        Map<String, String> stormConf = new HashMap<String, String>();
        stormConf.put(OpenNLPDictionaryExtractorGraphPropertyWorker.PATH_PREFIX_CONFIG, "file:///" + getClass().getResource(RESOURCE_CONFIG_DIR).getFile());
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
        assertEquals(3, termMentions.size());

        boolean found = false;
        for (TermMention term : termMentions) {
            if (term.getSign().equals("Bob Robertson")) {
                found = true;
                assertEquals(63, term.getStart());
                assertEquals(76, term.getEnd());
                break;
            }
        }
        assertTrue("Expected name not found!", found);

        ArrayList<String> signs = new ArrayList<String>();
        for (TermMention term : termMentions) {
            signs.add(term.getSign());
        }

        assertTrue("Bob Robertson not found", signs.contains("Bob Robertson"));
        assertTrue("Altamira Corporation not found", signs.contains("Altamira Corporation"));
        assertTrue("Boston , MA not found", signs.contains("Boston , MA"));
    }

    private List<TokenNameFinder> loadFinders() {
        List<TokenNameFinder> finders = new ArrayList<TokenNameFinder>();
        Dictionary people = new Dictionary();
        people.put(new StringList("Bob Robertson".split(" ")));
        finders.add(new DictionaryNameFinder(people, "person"));

        Dictionary locations = new Dictionary();
        locations.put(new StringList("Boston , MA".split(" ")));
        finders.add(new DictionaryNameFinder(locations, "location"));

        Dictionary organizations = new Dictionary();
        organizations.put(new StringList("Altamira Corporation".split(" ")));
        finders.add(new DictionaryNameFinder(organizations, "organization"));

        return finders;
    }
}
