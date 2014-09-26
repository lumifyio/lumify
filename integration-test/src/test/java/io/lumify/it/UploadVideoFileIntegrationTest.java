package io.lumify.it;

import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.properties.MediaLumifyProperties;
import io.lumify.web.clientapi.LumifyApi;
import io.lumify.web.clientapi.codegen.ApiException;
import io.lumify.web.clientapi.codegen.ArtifactApiExt;
import io.lumify.web.clientapi.codegen.model.ArtifactImportResponse;
import io.lumify.web.clientapi.codegen.model.Element;
import io.lumify.web.clientapi.codegen.model.Property;
import io.lumify.web.clientapi.codegen.model.WorkspaceDiff;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class UploadVideoFileIntegrationTest extends TestBase {
    private String artifactVertexId;

    @Test
    public void testUploadFile() throws IOException, ApiException {
        importVideoAndPublishAsUser1();
        resolveTermsAsUser1();
    }

    private void importVideoAndPublishAsUser1() throws ApiException, IOException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);
        addUserAuth(lumifyApi, USERNAME_TEST_USER_1, "auth1");

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

        WorkspaceDiff diff = lumifyApi.getWorkspaceApi().getDiff();
        System.out.println(diff);
        assertEquals(12, diff.getDiffs().size());
        lumifyApi.getWorkspaceApi().publishAll(diff.getDiffs());

        diff = lumifyApi.getWorkspaceApi().getDiff();
        System.out.println(diff);
        assertEquals(0, diff.getDiffs().size());

        lumifyApi.logout();
    }

    private void resolveTermsAsUser1() throws ApiException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);

        String propertyKey = "io.lumify.subrip.SubRipTranscriptGraphPropertyWorker";
        int videoFrameIndex = 0;
        int mentionStart = "".length();
        int mentionEnd = mentionStart + "Salam".length();
        lumifyApi.getEntityApi().resolveVideoTranscriptTerm(artifactVertexId, propertyKey, videoFrameIndex, mentionStart, mentionEnd, "Salam", CONCEPT_TEST_PERSON, "auth1");

        videoFrameIndex = 2;
        mentionStart = "appalling brutality what we know is that\nthree ".length();
        mentionEnd = mentionStart + "British".length();
        lumifyApi.getEntityApi().resolveVideoTranscriptTerm(artifactVertexId, propertyKey, videoFrameIndex, mentionStart, mentionEnd, "Great Britain", CONCEPT_TEST_PERSON, "auth1");

        lumifyTestCluster.processGraphPropertyQueue();

        String highlightedText = lumifyApi.getArtifactApi().getHighlightedText(artifactVertexId, propertyKey);
        System.out.println(highlightedText);
        assertTrue("missing highlighting for Salam", highlightedText.contains(">Salam<"));
        assertTrue("missing highlighting for British", highlightedText.contains("three <span") && highlightedText.contains(">British<"));

        lumifyApi.logout();
    }
}
