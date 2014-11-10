package io.lumify.it;

import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.properties.MediaLumifyProperties;
import io.lumify.tesseract.TesseractGraphPropertyWorker;
import io.lumify.web.clientapi.LumifyApi;
import io.lumify.web.clientapi.VertexApiExt;
import io.lumify.web.clientapi.codegen.ApiException;
import io.lumify.web.clientapi.model.ClientApiArtifactImportResponse;
import io.lumify.web.clientapi.model.ClientApiElement;
import io.lumify.web.clientapi.model.ClientApiProperty;
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
        ClientApiArtifactImportResponse artifact = lumifyApi.getVertexApi().importFiles(
                new VertexApiExt.FileForImport("auth1", "shortVideo.mp4", videoResourceStream),
                new VertexApiExt.FileForImport("auth1", "shortVideo.mp4.srt", videoTranscriptResourceStream));
        assertEquals(1, artifact.getVertexIds().size());
        artifactVertexId = artifact.getVertexIds().get(0);
        assertNotNull(artifactVertexId);

        lumifyTestCluster.processGraphPropertyQueue();

        boolean foundTesseractVideoTranscript = false;
        ClientApiElement vertex = lumifyApi.getVertexApi().getByVertexId(artifactVertexId);
        for (ClientApiProperty prop : vertex.getProperties()) {
            LOGGER.info(prop.toString());
            if (LumifyProperties.TEXT.getPropertyName().equals(prop.getName()) || MediaLumifyProperties.VIDEO_TRANSCRIPT.getPropertyName().equals(prop.getName())) {
                String highlightedText = lumifyApi.getVertexApi().getHighlightedText(artifactVertexId, prop.getKey());
                LOGGER.info("highlightedText: %s: %s: %s", prop.getName(), prop.getKey(), highlightedText);
                if (prop.getKey().equals(TesseractGraphPropertyWorker.TEXT_PROPERTY_KEY)) {
                    foundTesseractVideoTranscript = true;
                    assertTrue("invalid highlighted text for tesseract", highlightedText.contains("Test") && highlightedText.contains("12000"));
                }
            }
        }
        assertTrue("foundTesseractVideoTranscript", foundTesseractVideoTranscript);

        assertPublishAll(lumifyApi, 37);

        lumifyApi.logout();
    }

    private void assertRawRoute() throws ApiException, IOException {
        LOGGER.info("assertRawRoute");
        byte[] expected = IOUtils.toByteArray(UploadVideoFileIntegrationTest.class.getResourceAsStream("/io/lumify/it/shortVideo.mp4"));

        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);

        byte[] found = IOUtils.toByteArray(lumifyApi.getVertexApi().getRaw(artifactVertexId));
        assertArrayEquals(expected, found);

        lumifyApi.logout();
    }

    private void assertRawRoutePlayback() throws ApiException, IOException {
        LOGGER.info("assertRawRoutePlayback");
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);

        byte[] found = IOUtils.toByteArray(lumifyApi.getVertexApi().getRawForPlayback(artifactVertexId, MediaLumifyProperties.MIME_TYPE_VIDEO_MP4));
        assertTrue(found.length > 0);

        lumifyApi.logout();
    }

    private void assertPosterFrameRoute() throws ApiException, IOException {
        LOGGER.info("assertPosterFrameRoute");
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);

        InputStream in = lumifyApi.getVertexApi().getPosterFrame(artifactVertexId, 100);
        BufferedImage img = ImageIO.read(in);
        assertEquals(100, img.getWidth());
        assertEquals(66, img.getHeight());

        lumifyApi.logout();
    }

    private void assertVideoPreviewRoute() throws IOException, ApiException {
        LOGGER.info("assertVideoPreviewRoute");
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);

        InputStream in = lumifyApi.getVertexApi().getVideoPreview(artifactVertexId, 100);
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
        lumifyApi.getVertexApi().resolveVideoTranscriptTerm(artifactVertexId, propertyKey, videoFrameIndex, mentionStart, mentionEnd, "Salam", CONCEPT_TEST_PERSON, "auth1");

        videoFrameIndex = 2;
        mentionStart = "appalling brutality what we know is that\nthree ".length();
        mentionEnd = mentionStart + "British".length();
        lumifyApi.getVertexApi().resolveVideoTranscriptTerm(artifactVertexId, propertyKey, videoFrameIndex, mentionStart, mentionEnd, "Great Britain", CONCEPT_TEST_PERSON, "auth1");

        lumifyTestCluster.processGraphPropertyQueue();

        String highlightedText = lumifyApi.getVertexApi().getHighlightedText(artifactVertexId, propertyKey);
        LOGGER.info(highlightedText);
        assertTrue("missing highlighting for Salam", highlightedText.contains(">Salam<"));
        assertTrue("missing highlighting for British", highlightedText.contains("three <span") && highlightedText.contains(">British<"));

        lumifyApi.logout();
    }
}
