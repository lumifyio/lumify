package io.lumify.tikaTextExtractor;

import io.lumify.core.config.HashMapConfigurationLoader;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import io.lumify.core.model.audit.AuditRepository;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.security.DirectVisibilityTranslator;
import io.lumify.core.security.VisibilityTranslator;
import org.apache.commons.io.IOUtils;
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
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class TikaTextExtractorGraphPropertyWorkerTest {
    private Graph graph;
    private Visibility visibility;
    private Authorizations authorizations;
    private TikaTextExtractorGraphPropertyWorker textExtractor;
    private VisibilityTranslator visibilityTranslator;

    @Mock
    private WorkQueueRepository workQueueRepository;

    @Mock
    private AuditRepository auditRepository;

    @Before
    public void before() throws Exception {
        graph = InMemoryGraph.create();
        visibility = new Visibility("");
        authorizations = new InMemoryAuthorizations();
        textExtractor = new TikaTextExtractorGraphPropertyWorker();
        visibilityTranslator = new DirectVisibilityTranslator();

        Map config = new HashMap();
        config.put("ontology.iri.concept.person", "http://lumify.io/test#person");
        config.put("ontology.iri.concept.location", "http://lumify.io/test#location");
        config.put("ontology.iri.concept.organization", "http://lumify.io/test#organization");
        config.put("ontology.iri.relationship.artifactHasEntity", "http://lumify.io/test#artifactHasEntity");
        io.lumify.core.config.Configuration configuration = new HashMapConfigurationLoader(config).createConfiguration();

        GraphPropertyWorkerPrepareData prepareData = new GraphPropertyWorkerPrepareData(config, null, null, null, null, null);
        textExtractor.setConfiguration(configuration);
        textExtractor.setGraph(graph);
        textExtractor.setWorkQueueRepository(workQueueRepository);
        textExtractor.setAuditRepository(auditRepository);
        textExtractor.setVisibilityTranslator(visibilityTranslator);
        textExtractor.prepare(prepareData);
    }

    @Test
    public void testExtractWithHtml() throws Exception {
        String data = "<html>";
        data += "<head>";
        data += "<title>Test Title</title>";
        data += "<meta content=\"2013-01-01T18:09:20Z\" itemprop=\"datePublished\" name=\"pubdate\"/>";
        data += "</head>";
        data += "<body>";
        data += "<div><table><tr><td>Menu1</td><td>Menu2</td><td>Menu3</td></tr></table></div>\n";
        data += "\n";
        data += "<h1>Five reasons why Windows 8 has failed</h1>\n";
        data += "<p>The numbers speak for themselves. Vista, universally acknowledged as a failure, actually had significantly better adoption numbers than Windows 8. At similar points in their roll-outs, Vista had a desktop market share of 4.52% compared to Windows 8's share of 2.67%. Underlining just how poorly Windows 8's adoption has gone, Vista didn't even have the advantage of holiday season sales to boost its numbers. Tablets--and not Surface RT tablets--were what people bought last December, not Windows 8 PCs.</p>\n";
        data += "</body>";
        data += "</html>";
        createVertex(data, "text/html");

        InputStream in = new ByteArrayInputStream(data.getBytes());
        Vertex vertex = graph.getVertex("v1", authorizations);
        Property property = vertex.getProperty(LumifyProperties.RAW.getPropertyName());
        GraphPropertyWorkData workData = new GraphPropertyWorkData(visibilityTranslator, vertex, property, null, null);
        textExtractor.execute(in, workData);

        vertex = graph.getVertex("v1", authorizations);
        assertEquals("Test Title", LumifyProperties.TITLE.getPropertyValue(vertex));

        assertEquals(
                "Five reasons why Windows 8 has failed\n" +
                        "The numbers speak for themselves. Vista, universally acknowledged as a failure, actually had significantly better adoption numbers than Windows 8. At similar points in their roll-outs, Vista had a desktop market share of 4.52% compared to Windows 8's share of 2.67%. Underlining just how poorly Windows 8's adoption has gone, Vista didn't even have the advantage of holiday season sales to boost its numbers. Tablets--and not Surface RT tablets--were what people bought last December, not Windows 8 PCs.\n",
                IOUtils.toString(LumifyProperties.TEXT.getPropertyValue(vertex).getInputStream(), "UTF-8")
        );
        assertEquals(new Date(1357063760000L), LumifyProperties.CREATE_DATE.getPropertyValue(vertex));
    }

    private void createVertex(String data, String mimeType) throws UnsupportedEncodingException {
        VertexBuilder v = graph.prepareVertex("v1", visibility);
        StreamingPropertyValue textValue = new StreamingPropertyValue(new ByteArrayInputStream(data.getBytes("UTF-8")), byte[].class);
        textValue.searchIndex(false);
        Metadata metadata = new Metadata();
        metadata.add(LumifyProperties.MIME_TYPE.getPropertyName(), mimeType, visibilityTranslator.getDefaultVisibility());
        LumifyProperties.RAW.setProperty(v, textValue, metadata, visibility);
        v.save(authorizations);
    }

    @Test
    public void testExtractWithEmptyHtml() throws Exception {
        String data = "<html>";
        data += "<head>";
        data += "<title>Test Title</title>";
        data += "<meta content=\"2013-01-01T18:09:20Z\" itemprop=\"datePublished\" name=\"pubdate\"/>";
        data += "</head>";
        data += "<body>";
        data += "</body>";
        data += "</html>";
        createVertex(data, "text/html");

        InputStream in = new ByteArrayInputStream(data.getBytes());
        Vertex vertex = graph.getVertex("v1", authorizations);
        Property property = vertex.getProperty(LumifyProperties.RAW.getPropertyName());
        GraphPropertyWorkData workData = new GraphPropertyWorkData(visibilityTranslator, vertex, property, null, null);
        textExtractor.execute(in, workData);

        vertex = graph.getVertex("v1", authorizations);
        assertEquals("Test Title", LumifyProperties.TITLE.getPropertyValue(vertex));
        assertEquals("", IOUtils.toString(LumifyProperties.TEXT.getPropertyValue(vertex).getInputStream(), "UTF-8"));
        assertEquals(new Date(1357063760000L), LumifyProperties.CREATE_DATE.getPropertyValue(vertex));
    }

    @Test
    public void testExtractWithNotHtml() throws Exception {
        String data = "<title>Test Title</title>";
        data += "<meta content=\"2013-01-01T18:09:20Z\" itemprop=\"datePublished\" name=\"pubdate\"/>";
        data += "<h1>Five reasons why Windows 8 has failed</h1>";
        data += "<p>The numbers speak for themselves. Vista, universally acknowledged as a failure, actually had significantly better adoption numbers than Windows 8. At similar points in their roll-outs, Vista had a desktop market share of 4.52% compared to Windows 8's share of 2.67%. Underlining just how poorly Windows 8's adoption has gone, Vista didn't even have the advantage of holiday season sales to boost its numbers. Tablets--and not Surface RT tablets--were what people bought last December, not Windows 8 PCs.</p>";
        data += "</body>";
        data += "</html>";
        createVertex(data, "text/html");

        InputStream in = new ByteArrayInputStream(data.getBytes());
        Vertex vertex = graph.getVertex("v1", authorizations);
        Property property = vertex.getProperty(LumifyProperties.RAW.getPropertyName());
        GraphPropertyWorkData workData = new GraphPropertyWorkData(visibilityTranslator, vertex, property, null, null);
        textExtractor.execute(in, workData);

        vertex = graph.getVertex("v1", authorizations);
        assertEquals("Test Title", LumifyProperties.TITLE.getPropertyValue(vertex));
        assertEquals(
                "Five reasons why Windows 8 has failed\n" +
                        "The numbers speak for themselves. Vista, universally acknowledged as a failure, actually had significantly better adoption numbers than Windows 8. At similar points in their roll-outs, Vista had a desktop market share of 4.52% compared to Windows 8's share of 2.67%. Underlining just how poorly Windows 8's adoption has gone, Vista didn't even have the advantage of holiday season sales to boost its numbers. Tablets--and not Surface RT tablets--were what people bought last December, not Windows 8 PCs.\n",
                IOUtils.toString(LumifyProperties.TEXT.getPropertyValue(vertex).getInputStream(), "UTF-8")
        );
        assertEquals(new Date(1357063760000L), LumifyProperties.CREATE_DATE.getPropertyValue(vertex));
    }

    @Test
    public void testExtractTextWithAccentCharacters() throws Exception {
        String data = "the Quita Suena\u0301 bank";
        createVertex(data, "text/plain; charset=utf-8");

        InputStream in = new ByteArrayInputStream(data.getBytes("UTF-8"));
        Vertex vertex = graph.getVertex("v1", authorizations);
        Property property = vertex.getProperty(LumifyProperties.RAW.getPropertyName());
        GraphPropertyWorkData workData = new GraphPropertyWorkData(visibilityTranslator, vertex, property, null, null);
        textExtractor.execute(in, workData);

        vertex = graph.getVertex("v1", authorizations);
        String expected = "the Quita Suená bank ";
        String actual = IOUtils.toString(LumifyProperties.TEXT.getPropertyValue(vertex).getInputStream(), "UTF-8");
        assertEquals(21, expected.length());
        assertEquals(expected, actual);
        assertEquals(expected.length(), actual.length());
    }

    //todo : add test with image metadata
}
