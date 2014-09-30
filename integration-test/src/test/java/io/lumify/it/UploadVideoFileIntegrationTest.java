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
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class UploadVideoFileIntegrationTest extends TestBase {
    private String artifactVertexId;

    @Test
    public void testUploadFile() throws IOException, ApiException {
        importVideoAndPublishAsUser1();
        assertRawRoute();
        assertRawRoutePlayback();
        assertPosterFrameRoute();
        assertVideoPreviewRoute();
        resolveTermsAsUser1();
    }

    private void importVideoAndPublishAsUser1() throws ApiException, IOException {
        LOGGER.info("importVideoAndPublishAsUser1");
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
            LOGGER.info(prop.toString());
            if (LumifyProperties.TEXT.getPropertyName().equals(prop.getName()) || MediaLumifyProperties.VIDEO_TRANSCRIPT.getPropertyName().equals(prop.getName())) {
                LOGGER.info(lumifyApi.getArtifactApi().getHighlightedText(artifactVertexId, prop.getKey()));
            }
        }

        WorkspaceDiff diff = lumifyApi.getWorkspaceApi().getDiff();
        LOGGER.info("%s", diff.toString());
        assertEquals(16, diff.getDiffs().size());
        lumifyApi.getWorkspaceApi().publishAll(diff.getDiffs());

        diff = lumifyApi.getWorkspaceApi().getDiff();
        LOGGER.info("%s", diff.toString());
        assertEquals(0, diff.getDiffs().size());

        lumifyApi.logout();
    }

    private void assertRawRoute() throws ApiException, IOException {
        LOGGER.info("assertRawRoute");
        byte[] expected = IOUtils.toByteArray(UploadVideoFileIntegrationTest.class.getResourceAsStream("/io/lumify/it/shortVideo.mp4"));

        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);

        byte[] found = IOUtils.toByteArray(lumifyApi.getArtifactApi().getRaw(artifactVertexId));
        assertArrayEquals(expected, found);

        lumifyApi.logout();
    }

    private void assertRawRoutePlayback() throws ApiException, IOException {
        LOGGER.info("assertRawRoutePlayback");
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);

        byte[] found = IOUtils.toByteArray(lumifyApi.getArtifactApi().getRawForPlayback(artifactVertexId, MediaLumifyProperties.MIME_TYPE_VIDEO_MP4));
        assertTrue(found.length > 0);

        lumifyApi.logout();
    }

    private void assertPosterFrameRoute() throws ApiException, IOException {
        LOGGER.info("assertPosterFrameRoute");
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);

        InputStream in = lumifyApi.getArtifactApi().getPosterFrame(artifactVertexId, 100);
        BufferedImage img = ImageIO.read(in);
        assertEquals(100, img.getWidth());
        assertEquals(66, img.getHeight());

        lumifyApi.logout();
    }

    private void assertVideoPreviewRoute() throws IOException, ApiException {
        LOGGER.info("assertVideoPreviewRoute");
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);

        InputStream in = lumifyApi.getArtifactApi().getVideoPreview(artifactVertexId, 100);
        BufferedImage img = ImageIO.read(in);
        assertEquals(2000, img.getWidth());
        assertEquals(66, img.getHeight());

        lumifyApi.logout();
    }

    private void resolveTermsAsUser1() throws ApiException {
        LOGGER.info("resolveTermsAsUser1");
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
        LOGGER.info(highlightedText);
        assertTrue("missing highlighting for Salam", highlightedText.contains(">Salam<"));
        assertTrue("missing highlighting for British", highlightedText.contains("three <span") && highlightedText.contains(">British<"));

        lumifyApi.logout();
    }
}
