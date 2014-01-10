package com.altamiracorp.lumify.web.routes.artifact;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.servlet.http.HttpSession;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.altamiracorp.lumify.core.ingest.video.VideoPlaybackDetails;
import com.altamiracorp.lumify.core.model.artifact.Artifact;
import com.altamiracorp.lumify.core.model.artifact.ArtifactRowKey;
import com.altamiracorp.lumify.core.model.graph.GraphRepository;
import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.web.AuthenticationProvider;
import com.altamiracorp.lumify.web.routes.RouteTestBase;

@RunWith(MockitoJUnitRunner.class)
public class ArtifactRawByRowKeyTest extends RouteTestBase {
    private ArtifactRawByRowKey artifactRawByRowKey;

    @Mock
    private GraphVertex vertex;

    @Mock
    private VideoPlaybackDetails mockVideoDetails;

    @Mock
    private GraphRepository mockGraphRepository;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        artifactRawByRowKey = new ArtifactRawByRowKey(mockArtifactRepository, mockGraphRepository);
    }

    @Test
    public void testHandleTextFile() throws Exception {
        ArtifactRowKey artifactRowKey = ArtifactRowKey.build("testContents".getBytes());
        when(mockRequest.getParameter("download")).thenReturn(null);
        when(mockRequest.getParameter("playback")).thenReturn(null);
        when(mockRequest.getAttribute("_rowKey")).thenReturn(artifactRowKey.toString());
        when(mockHttpSession.getAttribute(AuthenticationProvider.CURRENT_USER_REQ_ATTR_NAME)).thenReturn(mockUser);

        Artifact artifact = new Artifact(artifactRowKey);
        artifact.getMetadata()
                .setGraphVertexId("123")
                .setFileName("testFile.testExt")
                .setFileExtension("testExt_2014-01-08T13-24-09-0500")
                .setMimeType("text/plain");
        when(mockArtifactRepository.findByRowKey(artifactRowKey.toString(), mockUser.getModelUserContext())).thenReturn(artifact);
        when(mockGraphRepository.findVertex(artifact.getMetadata().getGraphVertexId(), mockUser)).thenReturn(vertex);

        InputStream testInputStream = new ByteArrayInputStream("test data".getBytes());
        when(mockArtifactRepository.getRaw(artifact, vertex, mockUser)).thenReturn(testInputStream);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                byte[] data = (byte[]) invocation.getArguments()[0];
                int start = (Integer) invocation.getArguments()[1];
                int len = (Integer) invocation.getArguments()[2];

                assertEquals(0, start);
                assertEquals(9, len);
                assertEquals("test data", new String(data, start, len));
                return null;
            }
        }).when(mockResponseOutputStream).write(any(byte[].class), any(Integer.class), any(Integer.class));

        artifactRawByRowKey.handle(mockRequest, mockResponse, mockHandlerChain);

        verify(mockResponse).setContentType("text/plain");
        verify(mockResponse).addHeader("Content-Disposition", "inline; filename=testFile.testExt");
    }

    @Test
    public void testHandleVideoPlayback() throws Exception {
        ArtifactRowKey artifactRowKey = ArtifactRowKey.build("testContents".getBytes());
        when(mockRequest.getParameter("download")).thenReturn(null);
        when(mockRequest.getParameter("playback")).thenReturn("true");
        when(mockRequest.getParameter("type")).thenReturn("video/mp4");
        when(mockRequest.getHeader("Range")).thenReturn("bytes=1-4");
        when(mockRequest.getAttribute("_rowKey")).thenReturn(artifactRowKey.toString());
        when(mockHttpSession.getAttribute(AuthenticationProvider.CURRENT_USER_REQ_ATTR_NAME)).thenReturn(mockUser);

        Artifact artifact = new Artifact(artifactRowKey);
        artifact.getMetadata()
                .setGraphVertexId("123")
                .setFileName("testFile.testExt")
                .setFileExtension("testExt_2014-01-08T13-24-09-0500")
                .setMimeType("video/mp4");
        when(mockArtifactRepository.findByRowKey(artifactRowKey.toString(), mockUser.getModelUserContext())).thenReturn(artifact);
        when(mockGraphRepository.findVertex(artifact.getMetadata().getGraphVertexId(), mockUser)).thenReturn(vertex);

        InputStream testInputStream = new ByteArrayInputStream("test data".getBytes());
        when(mockVideoDetails.getVideoStream()).thenReturn(testInputStream);
        when(mockVideoDetails.getVideoFileSize()).thenReturn((long) "test data".length());

        when(mockArtifactRepository.getVideoPlaybackDetails(anyString(), anyString())).thenReturn(mockVideoDetails);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                byte[] data = (byte[]) invocation.getArguments()[0];
                int start = (Integer) invocation.getArguments()[1];
                int len = (Integer) invocation.getArguments()[2];

                assertEquals(0, start);
                assertEquals(4, len);
                assertEquals("est ", new String(data, start, len));
                return null;
            }
        }).when(mockResponseOutputStream).write(any(byte[].class), any(Integer.class), any(Integer.class));

        artifactRawByRowKey.handle(mockRequest, mockResponse, mockHandlerChain);

        verify(mockResponse).setContentType("video/mp4");
        verify(mockResponse).addHeader("Content-Disposition", "attachment; filename=testFile.testExt");
        verify(mockResponse).addHeader("Content-Length", "4");
        verify(mockResponse).addHeader("Content-Range", "bytes 1-4/9");
    }
}
