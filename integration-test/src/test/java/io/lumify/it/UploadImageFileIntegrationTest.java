package io.lumify.it;

import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.properties.MediaLumifyProperties;
import io.lumify.web.clientapi.LumifyApi;
import io.lumify.web.clientapi.codegen.ApiException;
import io.lumify.web.clientapi.codegen.ArtifactApiExt;
import io.lumify.web.clientapi.codegen.model.*;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class UploadImageFileIntegrationTest extends TestBase {
    private String artifactVertexId;

    @Test
    public void testUploadFile() throws IOException, ApiException {
        importImageAndPublishAsUser1();
        assertRawRoute();
        assertThumbnailRoute();
        resolveDetectedObject();
        unresolveDetectedObject();
    }

    private void importImageAndPublishAsUser1() throws ApiException, IOException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);
        addUserAuth(lumifyApi, USERNAME_TEST_USER_1, "auth1");

        InputStream imageResourceStream = UploadImageFileIntegrationTest.class.getResourceAsStream("/io/lumify/it/sampleImage.jpg");
        ArtifactImportResponse artifact = lumifyApi.getArtifactApi().importFiles(
                new ArtifactApiExt.FileForImport("auth1", "sampleImage.jpg", imageResourceStream));
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

        DetectedObjects detectedObjects = lumifyApi.getVertexApi().getDetectedObjects(artifactVertexId, LumifyProperties.DETECTED_OBJECT.getPropertyName(), "");
        assertEquals(15, detectedObjects.getDetectedObjects().size());

        WorkspaceDiff diff = lumifyApi.getWorkspaceApi().getDiff();
        LOGGER.info("%s", diff.toString());
        assertEquals(9, diff.getDiffs().size());
        lumifyApi.getWorkspaceApi().publishAll(diff.getDiffs());

        diff = lumifyApi.getWorkspaceApi().getDiff();
        LOGGER.info("%s", diff.toString());
        assertEquals(0, diff.getDiffs().size());

        lumifyApi.logout();
    }

    private void assertRawRoute() throws ApiException, IOException {
        byte[] expected = IOUtils.toByteArray(UploadImageFileIntegrationTest.class.getResourceAsStream("/io/lumify/it/sampleImage.jpg"));

        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);

        byte[] found = IOUtils.toByteArray(lumifyApi.getArtifactApi().getRaw(artifactVertexId));
        assertArrayEquals(expected, found);

        lumifyApi.logout();
    }

    private void assertThumbnailRoute() throws ApiException, IOException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);

        InputStream in = lumifyApi.getArtifactApi().getThumbnail(artifactVertexId, 100);
        BufferedImage img = ImageIO.read(in);
        assertEquals(53, img.getWidth());
        assertEquals(100, img.getHeight());

        lumifyApi.logout();
    }

    private void resolveDetectedObject() throws ApiException {
        // TODO write me
    }

    private void unresolveDetectedObject() {
        // TODO write me
    }
}
