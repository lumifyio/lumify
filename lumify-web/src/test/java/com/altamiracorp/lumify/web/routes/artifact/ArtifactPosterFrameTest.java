package com.altamiracorp.lumify.web.routes.artifact;

import com.altamiracorp.lumify.core.model.artifact.ArtifactRowKey;
import com.altamiracorp.lumify.core.model.artifactThumbnails.ArtifactThumbnailRepository;
import com.altamiracorp.lumify.web.AuthenticationProvider;
import com.altamiracorp.lumify.web.routes.RouteTestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ArtifactPosterFrameTest extends RouteTestBase {
    private ArtifactPosterFrame artifactPosterFrame;

    @Override
    @Before
    public void setUp() throws Exception {
//        super.setUp();

        final ArtifactThumbnailRepository mockThumbnailRepository = Mockito.mock(ArtifactThumbnailRepository.class);
        artifactPosterFrame = new ArtifactPosterFrame(mockArtifactRepository, mockThumbnailRepository);
    }

    @Test
    public void testHandle() throws Exception {
        // TODO rewrite this test for secure graph!!!
//        ArtifactRowKey artifactRowKey = ArtifactRowKey.build("testContents".getBytes());
//        when(mockRequest.getAttribute("graphVertexId")).thenReturn("id");
//        when(mockHttpSession.getAttribute(AuthenticationProvider.CURRENT_USER_REQ_ATTR_NAME)).thenReturn(mockUser);
//
//        when(mockArtifactRepository.findRowKeyByGraphVertexId("id", mockUser)).thenReturn(artifactRowKey);
//
//        InputStream testInputStream = new ByteArrayInputStream("test data".getBytes());
//        when(mockArtifactRepository.getRawPosterFrame(artifactRowKey.toString())).thenReturn(testInputStream);
//
//        doAnswer(new Answer<Void>() {
//            @Override
//            public Void answer(InvocationOnMock invocation) throws Throwable {
//                byte[] data = (byte[]) invocation.getArguments()[0];
//                int start = (Integer) invocation.getArguments()[1];
//                int len = (Integer) invocation.getArguments()[2];
//
//                assertEquals(0, start);
//                assertEquals(9, len);
//                assertEquals("test data", new String(data, start, len));
//                return null;
//            }
//        }).when(mockResponseOutputStream).write(any(byte[].class), any(Integer.class), any(Integer.class));
//
//        artifactPosterFrame.handle(mockRequest, mockResponse, mockHandlerChain);
//
//        verify(mockResponse).setContentType("image/png");
    }
}
