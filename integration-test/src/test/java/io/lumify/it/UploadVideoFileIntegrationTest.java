package io.lumify.it;

import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.properties.MediaLumifyProperties;
import io.lumify.web.clientapi.LumifyApi;
import io.lumify.web.clientapi.codegen.ApiException;
import io.lumify.web.clientapi.codegen.ArtifactApiExt;
import io.lumify.web.clientapi.codegen.model.ArtifactImportResponse;
import io.lumify.web.clientapi.codegen.model.Element;
import io.lumify.web.clientapi.codegen.model.Property;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class UploadVideoFileIntegrationTest extends TestBase {
    private String user2Id;
    private String workspaceId;
    private String artifactVertexId;

    @Test
    public void testUploadFile() throws IOException, ApiException {
        importArtifactAsUser1();
    }

    public void importArtifactAsUser1() throws ApiException, IOException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);
        addUserAuth(lumifyApi, USERNAME_TEST_USER_1, "auth1");
        workspaceId = lumifyApi.getCurrentWorkspaceId();

        InputStream videoResourceStream = UploadVideoFileIntegrationTest.class.getResourceAsStream("/io/lumify/it/shortVideo.mp4");
        InputStream videoTranscriptResourceStream = UploadVideoFileIntegrationTest.class.getResourceAsStream("/io/lumify/it/shortVideo.mp4.srt");
        ArtifactImportResponse artifact = lumifyApi.getArtifactApi().importFiles(
                new ArtifactApiExt.FileForImport("auth1", "shortVideo.mp4", videoResourceStream),
                new ArtifactApiExt.FileForImport("auth1", "shortVideo.mp4.srt", videoTranscriptResourceStream));
        assertEquals(1, artifact.getVertexIds().size());
        artifactVertexId = artifact.getVertexIds().get(0);
        assertNotNull(artifactVertexId);

        lumifyTestCluster.processGraphPropertyQueue();

        Element vertex = lumifyApi.getVertexApi().getByVertexId(artifactVertexId);
        for (Property prop : vertex.getProperties()) {
            System.out.println(prop.toString());
            if (LumifyProperties.TEXT.getPropertyName().equals(prop.getName()) || MediaLumifyProperties.VIDEO_TRANSCRIPT.getPropertyName().equals(prop.getName())) {
                System.out.println(lumifyApi.getArtifactApi().getHighlightedText(artifactVertexId, prop.getKey()));
            }
        }

        lumifyApi.logout();
    }
}
