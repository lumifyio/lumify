package io.lumify.knownEntity;

import com.google.inject.Injector;
import io.lumify.core.config.HashMapConfigurationLoader;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import io.lumify.core.ingest.graphProperty.TermMentionFilter;
import io.lumify.core.ingest.term.extraction.TermMention;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.user.User;
import org.apache.hadoop.conf.Configuration;
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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.securegraph.util.IterableUtils.toList;

@RunWith(MockitoJUnitRunner.class)
public class KnownEntityExtractorGraphPropertyWorkerTest {
    private KnownEntityExtractorGraphPropertyWorker extractor;

    @Mock
    private User user;
    String dictionaryPath;
    List<TermMention> termMentions;
    private InMemoryAuthorizations authorizations;
    private InMemoryGraph graph;
    private Visibility visibility;

    @Before
    public void setup() throws Exception {
        Map config = new HashMap();
        config.put(io.lumify.core.config.Configuration.ONTOLOGY_IRI_ARTIFACT_HAS_ENTITY, "http://lumify.io/test#artifactHasEntity");
        io.lumify.core.config.Configuration configuration = new HashMapConfigurationLoader(config).createConfiguration();

        dictionaryPath = getClass().getResource(".").getPath();
        extractor = new KnownEntityExtractorGraphPropertyWorker() {
            @Override
            protected List<TermMentionWithGraphVertex> saveTermMentions(Vertex artifactGraphVertex, Iterable<TermMention> termMentions) {
                KnownEntityExtractorGraphPropertyWorkerTest.this.termMentions = toList(termMentions);
                return null;
            }
        };
        extractor.setConfiguration(configuration);

        Map<String, String> stormConf = new HashMap<String, String>();
        stormConf.put(KnownEntityExtractorGraphPropertyWorker.PATH_PREFIX_CONFIG, "file://" + dictionaryPath);
        FileSystem hdfsFileSystem = FileSystem.get(new Configuration());
        authorizations = new InMemoryAuthorizations();
        Injector injector = null;
        List<TermMentionFilter> termMentionFilters = new ArrayList<TermMentionFilter>();
        GraphPropertyWorkerPrepareData workerPrepareData = new GraphPropertyWorkerPrepareData(stormConf, termMentionFilters, hdfsFileSystem, user, authorizations, injector);
        graph = new InMemoryGraph();
        visibility = new Visibility("");
        extractor.prepare(workerPrepareData);
        extractor.setGraph(graph);
    }

    @Test
    public void textExtract() throws Exception {
        InputStream in = getClass().getResourceAsStream("bffls.txt");
        VertexBuilder vertexBuilder = graph.prepareVertex("v1", visibility);
        StreamingPropertyValue textPropertyValue = new StreamingPropertyValue(in, String.class);
        LumifyProperties.TEXT.setProperty(vertexBuilder, textPropertyValue, visibility);
        Vertex vertex = vertexBuilder.save(authorizations);

        in = getClass().getResourceAsStream("bffls.txt");
        Property property = vertex.getProperty(LumifyProperties.TEXT.getPropertyName());
        GraphPropertyWorkData workData = new GraphPropertyWorkData(vertex, property);
        extractor.execute(in, workData);
        assertEquals(3, termMentions.size());
        for (TermMention termMention : termMentions) {
            assertTrue(termMention.isResolved());
            assertEquals("person", termMention.getOntologyClassUri());
            assertEquals("Joe Ferner", termMention.getSign());
        }
    }
}
