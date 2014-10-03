package io.lumify.knownEntity;

import com.google.inject.Injector;
import io.lumify.core.config.HashMapConfigurationLoader;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import io.lumify.core.ingest.graphProperty.TermMentionFilter;
import io.lumify.core.model.audit.AuditRepository;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.termMention.TermMentionRepository;
import io.lumify.core.security.DirectVisibilityTranslator;
import io.lumify.core.security.VisibilityTranslator;
import io.lumify.core.user.User;
import org.apache.hadoop.conf.Configuration;
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

    @Mock
    private AuditRepository auditRepostiory;

    String dictionaryPath;
    private InMemoryAuthorizations authorizations;
    private InMemoryAuthorizations termMentionAuthorizations;
    private InMemoryGraph graph;
    private Visibility visibility;
    private VisibilityTranslator visibilityTranslator = new DirectVisibilityTranslator();

    @Before
    public void setup() throws Exception {
        Map config = new HashMap();
        config.put(io.lumify.core.config.Configuration.ONTOLOGY_IRI_PERSON, "http://lumify.io/test#person");
        config.put(io.lumify.core.config.Configuration.ONTOLOGY_IRI_LOCATION, "http://lumify.io/test#location");
        config.put(io.lumify.core.config.Configuration.ONTOLOGY_IRI_ORGANIZATION, "http://lumify.io/test#organization");
        config.put(io.lumify.core.config.Configuration.ONTOLOGY_IRI_ARTIFACT_HAS_ENTITY, "http://lumify.io/test#artifactHasEntity");
        io.lumify.core.config.Configuration configuration = new HashMapConfigurationLoader(config).createConfiguration();

        dictionaryPath = getClass().getResource(".").getPath();
        extractor = new KnownEntityExtractorGraphPropertyWorker();
        extractor.setAuditRepository(auditRepostiory);
        extractor.setVisibilityTranslator(visibilityTranslator);
        extractor.setConfiguration(configuration);

        config.put(KnownEntityExtractorGraphPropertyWorker.PATH_PREFIX_CONFIG, "file://" + dictionaryPath);
        FileSystem hdfsFileSystem = FileSystem.get(new Configuration());
        authorizations = new InMemoryAuthorizations();
        termMentionAuthorizations = new InMemoryAuthorizations(TermMentionRepository.VISIBILITY);
        Injector injector = null;
        List<TermMentionFilter> termMentionFilters = new ArrayList<TermMentionFilter>();
        GraphPropertyWorkerPrepareData workerPrepareData = new GraphPropertyWorkerPrepareData(config, termMentionFilters, hdfsFileSystem, user, authorizations, injector);
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
        GraphPropertyWorkData workData = new GraphPropertyWorkData(vertex, property, null, null);
        extractor.execute(in, workData);

        List<Vertex> termMentions = toList(vertex.getVertices(Direction.OUT, LumifyProperties.TERM_MENTION_LABEL_HAS_TERM_MENTION, termMentionAuthorizations));

        assertEquals(3, termMentions.size());
        for (Vertex termMention : termMentions) {
            assertTrue(LumifyProperties.TERM_MENTION_RESOLVED_EDGE_ID.getPropertyValue(termMention) != null);
            assertEquals("http://lumify.io/test#person", LumifyProperties.TERM_MENTION_CONCEPT_TYPE.getPropertyValue(termMention));
            assertEquals("Joe Ferner", LumifyProperties.TERM_MENTION_TITLE.getPropertyValue(termMention));
        }
    }
}
