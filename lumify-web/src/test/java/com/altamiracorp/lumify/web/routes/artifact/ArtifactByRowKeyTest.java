package com.altamiracorp.lumify.web.routes.artifact;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpSession;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.altamiracorp.lumify.core.model.artifact.Artifact;
import com.altamiracorp.lumify.core.model.artifact.ArtifactRowKey;
import com.altamiracorp.lumify.core.model.graph.GraphRepository;
import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.web.AuthenticationProvider;
import com.altamiracorp.lumify.web.routes.RouteTestBase;
import com.google.common.net.MediaType;

@RunWith(MockitoJUnitRunner.class)
public class ArtifactByRowKeyTest extends RouteTestBase {
    private ArtifactByRowKey artifactByRowKey;

    @Mock
    private User user;

    @Mock
    private HttpSession mockSession;

    @Mock
    private GraphRepository mockGraphRepository;

    @Mock
    private GraphVertex mockVertex;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        artifactByRowKey = new ArtifactByRowKey(mockArtifactRepository, mockGraphRepository);
    }

    @Test
    public void testHandle() throws Exception {
        ArtifactRowKey artifactRowKey = ArtifactRowKey.build("testContents".getBytes());
        when(mockRequest.getAttribute("_rowKey")).thenReturn(artifactRowKey.toString());
        when(mockRequest.getSession()).thenReturn(mockSession);
        when(mockSession.getAttribute(AuthenticationProvider.CURRENT_USER_REQ_ATTR_NAME)).thenReturn(user);

        Artifact artifact = new Artifact(artifactRowKey);
        artifact.getMetadata().setMimeType(MediaType.PDF.toString());

        when(mockVertex.getProperty(PropertyName.SUBTYPE)).thenReturn("document");
        when(mockGraphRepository.findVertexByRowKey(artifactRowKey.toString(), user)).thenReturn(mockVertex);
        when(mockArtifactRepository.findByRowKey(artifactRowKey.toString(), user.getModelUserContext())).thenReturn(artifact);

        artifactByRowKey.handle(mockRequest, mockResponse, mockHandlerChain);

        JSONObject responseJson = new JSONObject(responseStringWriter.getBuffer().toString());
        assertEquals(ArtifactRawByRowKey.getUrl(artifactRowKey), responseJson.getString("rawUrl"));
        assertEquals(artifactRowKey.toString(), responseJson.getJSONObject("key").getString("value"));
    }
}