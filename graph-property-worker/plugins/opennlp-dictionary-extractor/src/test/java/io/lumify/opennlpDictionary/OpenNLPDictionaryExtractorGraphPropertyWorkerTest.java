package io.lumify.opennlpDictionary;

import com.google.inject.Injector;
import io.lumify.core.config.HashMapConfigurationLoader;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import io.lumify.core.ingest.graphProperty.TermMentionFilter;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.termMention.TermMentionRepository;
import io.lumify.core.model.user.AuthorizationRepository;
import io.lumify.core.model.user.InMemoryAuthorizationRepository;
import io.lumify.core.security.DirectVisibilityTranslator;
import io.lumify.core.security.VisibilityTranslator;
import io.lumify.core.user.User;
import io.lumify.opennlpDictionary.model.DictionaryEntryRepository;
import opennlp.tools.dictionary.Dictionary;
import opennlp.tools.namefind.DictionaryNameFinder;
import opennlp.tools.namefind.TokenNameFinder;
import opennlp.tools.util.StringList;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.securegraph.Vertex;
import org.securegraph.Visibility;
import org.securegraph.inmemory.InMemoryAuthorizations;
import org.securegraph.inmemory.InMemoryGraph;
import org.securegraph.inmemory.InMemoryVertex;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.securegraph.util.IterableUtils.toList;

@RunWith(MockitoJUnitRunner.class)
public class OpenNLPDictionaryExtractorGraphPropertyWorkerTest {
    private static final String RESOURCE_CONFIG_DIR = "/fs/conf/opennlp";

    private OpenNLPDictionaryExtractorGraphPropertyWorker extractor;

    @Mock
    private User user;

    private InMemoryAuthorizations authorizations;

    @Mock
    private DictionaryEntryRepository dictionaryEntryRepository;

    private InMemoryGraph graph;
    private VisibilityTranslator visibilityTranslator = new DirectVisibilityTranslator();
    private TermMentionRepository termMentionRepository;

    @Before
    public void setUp() throws Exception {
        final List<TokenNameFinder> finders = loadFinders();

        Map config = new HashMap();
        config.put("ontology.iri.concept.person", "http://lumify.io/test#person");
        config.put("ontology.iri.concept.location", "http://lumify.io/test#location");
        config.put("ontology.iri.concept.organization", "http://lumify.io/test#organization");
        config.put("ontology.iri.relationship.artifactHasEntity", "http://lumify.io/test#artifactHasEntity");
        io.lumify.core.config.Configuration configuration = new HashMapConfigurationLoader(config).createConfiguration();

        graph = InMemoryGraph.create();

        extractor = new OpenNLPDictionaryExtractorGraphPropertyWorker() {
            @Override
            protected List<TokenNameFinder> loadFinders() throws IOException {
                return finders;
            }
        };
        extractor.setConfiguration(configuration);
        extractor.setDictionaryEntryRepository(dictionaryEntryRepository);
        extractor.setVisibilityTranslator(visibilityTranslator);
        extractor.setGraph(graph);

        AuthorizationRepository authorizationRepository = new InMemoryAuthorizationRepository();
        termMentionRepository = new TermMentionRepository(graph, authorizationRepository);

        config.put(OpenNLPDictionaryExtractorGraphPropertyWorker.PATH_PREFIX_CONFIG, "file:///" + getClass().getResource(RESOURCE_CONFIG_DIR).getFile());
        FileSystem hdfsFileSystem = FileSystem.get(new Configuration());
        authorizations = new InMemoryAuthorizations();
        Injector injector = null;
        List<TermMentionFilter> termMentionFilters = new ArrayList<>();
        GraphPropertyWorkerPrepareData workerPrepareData = new GraphPropertyWorkerPrepareData(config, termMentionFilters, hdfsFileSystem, user, authorizations, injector);
        extractor.prepare(workerPrepareData);
    }

    @Test
    public void testEntityExtraction() throws Exception {
        JSONObject visibilityJson = new JSONObject();
        visibilityJson.put("source", "");
        InMemoryVertex vertex = (InMemoryVertex) graph.prepareVertex("v1", new Visibility(""))
                .setProperty("text", "none", new Visibility(""))
                .setProperty(LumifyProperties.VISIBILITY_JSON.getPropertyName(), visibilityJson, new Visibility(""))
                .save(new InMemoryAuthorizations());
        graph.flush();

        GraphPropertyWorkData workData = new GraphPropertyWorkData(visibilityTranslator, vertex, vertex.getProperty("text"), null, "");
        String text = "This is a sentence that is going to tell you about a guy named "
                + "Bob Robertson who lives in Boston, MA and works for a company called Altamira Corporation";
        extractor.execute(new ByteArrayInputStream(text.getBytes()), workData);

        List<Vertex> termMentions = toList(termMentionRepository.findBySourceGraphVertex(vertex.getId(), authorizations));

        assertEquals(3, termMentions.size());

        boolean found = false;
        for (Vertex term : termMentions) {
            String title = LumifyProperties.TERM_MENTION_TITLE.getPropertyValue(term);
            if (title.equals("Bob Robertson")) {
                found = true;
                assertEquals(63, LumifyProperties.TERM_MENTION_START_OFFSET.getPropertyValue(term, 0));
                assertEquals(76, LumifyProperties.TERM_MENTION_END_OFFSET.getPropertyValue(term, 0));
                break;
            }
        }
        assertTrue("Expected name not found!", found);

        ArrayList<String> signs = new ArrayList<>();
        for (Vertex term : termMentions) {
            String title = LumifyProperties.TERM_MENTION_TITLE.getPropertyValue(term);
            signs.add(title);
        }

        assertTrue("Bob Robertson not found", signs.contains("Bob Robertson"));
        assertTrue("Altamira Corporation not found", signs.contains("Altamira Corporation"));
        assertTrue("Boston , MA not found", signs.contains("Boston , MA"));
    }

    private List<TokenNameFinder> loadFinders() {
        List<TokenNameFinder> finders = new ArrayList<>();
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
