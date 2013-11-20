package com.altamiracorp.lumify.textExtraction;

import com.altamiracorp.lumify.core.ingest.ArtifactExtractedInfo;
import com.altamiracorp.bigtable.model.MockSession;
import com.altamiracorp.lumify.core.model.artifact.Artifact;
import com.altamiracorp.lumify.core.model.artifact.ArtifactRepository;
import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.ModelUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TikaTextExtractorTest {
    private MockSession session;

    @Mock
    private User user;

    @Mock GraphVertex vertex;

    @Mock
    private ArtifactRepository artifactRepository;

    OutputStream textOut;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        session = new MockSession();
        ModelUtil.initializeTables(session,user);
        textOut = new ByteArrayOutputStream();
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

        TikaTextExtractor textExtractor = new TikaTextExtractor();
        ArtifactExtractedInfo info = textExtractor.extract(new ByteArrayInputStream(data.getBytes()), "text/html", textOut);
        info.setText(textOut.toString());
        assertEquals("Test Title", info.getTitle());

        assertEquals(
                "Five reasons why Windows 8 has failed\n" +
                        "The numbers speak for themselves. Vista, universally acknowledged as a failure, actually had significantly better adoption numbers than Windows 8. At similar points in their roll-outs, Vista had a desktop market share of 4.52% compared to Windows 8's share of 2.67%. Underlining just how poorly Windows 8's adoption has gone, Vista didn't even have the advantage of holiday season sales to boost its numbers. Tablets--and not Surface RT tablets--were what people bought last December, not Windows 8 PCs.\n",
                info.getText());
        assertEquals(new Date(1357063760000L), info.getDate());
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

        TikaTextExtractor textExtractor = new TikaTextExtractor();
        ArtifactExtractedInfo info = textExtractor.extract(new ByteArrayInputStream(data.getBytes()), "text/html", textOut);
        info.setText(textOut.toString());
        assertEquals("Test Title", info.getTitle());
        assertEquals("", info.getText());
        assertEquals(new Date(1357063760000L), info.getDate());
    }

    @Test
    public void testExtractWithNotHtml() throws Exception {
        String data = "<title>Test Title</title>";
        data += "<meta content=\"2013-01-01T18:09:20Z\" itemprop=\"datePublished\" name=\"pubdate\"/>";
        data += "<h1>Five reasons why Windows 8 has failed</h1>";
        data += "<p>The numbers speak for themselves. Vista, universally acknowledged as a failure, actually had significantly better adoption numbers than Windows 8. At similar points in their roll-outs, Vista had a desktop market share of 4.52% compared to Windows 8's share of 2.67%. Underlining just how poorly Windows 8's adoption has gone, Vista didn't even have the advantage of holiday season sales to boost its numbers. Tablets--and not Surface RT tablets--were what people bought last December, not Windows 8 PCs.</p>";
        data += "</body>";
        data += "</html>";

        TikaTextExtractor textExtractor = new TikaTextExtractor();
        Artifact artifact = new Artifact();
        when(artifactRepository.getRaw(eq(artifact), eq(vertex), eq(user))).thenReturn(new ByteArrayInputStream(data.getBytes()));
        ArtifactExtractedInfo info = textExtractor.extract(new ByteArrayInputStream(data.getBytes()), "text/plain", textOut);
        info.setText(textOut.toString());
        assertEquals("Test Title", info.getTitle());
        assertEquals(
                "Five reasons why Windows 8 has failed\n" +
                        "The numbers speak for themselves. Vista, universally acknowledged as a failure, actually had significantly better adoption numbers than Windows 8. At similar points in their roll-outs, Vista had a desktop market share of 4.52% compared to Windows 8's share of 2.67%. Underlining just how poorly Windows 8's adoption has gone, Vista didn't even have the advantage of holiday season sales to boost its numbers. Tablets--and not Surface RT tablets--were what people bought last December, not Windows 8 PCs.\n",
                info.getText());
        assertEquals(new Date(1357063760000L), info.getDate());
    }

    //todo : add test with image metadata
}
