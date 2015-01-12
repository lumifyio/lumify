package io.lumify.it;

import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.properties.MediaLumifyProperties;
import io.lumify.tesseract.TesseractGraphPropertyWorker;
import io.lumify.web.clientapi.LumifyApi;
import io.lumify.web.clientapi.VertexApiExt;
import io.lumify.web.clientapi.codegen.ApiException;
import io.lumify.web.clientapi.model.*;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class UploadImageFileIntegrationTest extends TestBase {
    private String artifactVertexId;

    @Test
    public void testUploadFile() throws IOException, ApiException {
        importImageAndPublishAsUser1();
        assertRawRoute();
        assertThumbnailRoute();
        resolveAndUnresolveDetectedObject();
    }

    private void importImageAndPublishAsUser1() throws ApiException, IOException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);
        addUserAuths(lumifyApi, USERNAME_TEST_USER_1, "auth1");

        InputStream imageResourceStream = UploadImageFileIntegrationTest.class.getResourceAsStream("/io/lumify/it/sampleImage.jpg");
        ClientApiArtifactImportResponse artifact = lumifyApi.getVertexApi().importFiles(
                new VertexApiExt.FileForImport("auth1", "sampleImage.jpg", imageResourceStream));
        assertEquals(1, artifact.getVertexIds().size());
        artifactVertexId = artifact.getVertexIds().get(0);
        assertNotNull(artifactVertexId);

        lumifyTestCluster.processGraphPropertyQueue();

        ClientApiElement vertex = lumifyApi.getVertexApi().getByVertexId(artifactVertexId);
        boolean foundTesseractText = false;
        for (ClientApiProperty prop : vertex.getProperties()) {
            LOGGER.info(prop.toString());
            if (LumifyProperties.TEXT.getPropertyName().equals(prop.getName()) || MediaLumifyProperties.VIDEO_TRANSCRIPT.getPropertyName().equals(prop.getName())) {
                String highlightedText = lumifyApi.getVertexApi().getHighlightedText(artifactVertexId, prop.getKey());
                LOGGER.info("highlightedText: %s: %s", prop.getKey(), highlightedText);
                if (prop.getKey().startsWith(TesseractGraphPropertyWorker.TEXT_PROPERTY_KEY)) {
                    assertTrue("invalid tesseract text", highlightedText.contains("WORLD SERIES GAME 7"));
                    foundTesseractText = true;
                }
            }
        }
        assertTrue("could not find TesseractText", foundTesseractText);

        assertPublishAll(lumifyApi, 41);

        lumifyApi.logout();
    }

    private void assertRawRoute() throws ApiException, IOException {
        byte[] expected = IOUtils.toByteArray(UploadImageFileIntegrationTest.class.getResourceAsStream("/io/lumify/it/sampleImage.jpg"));

        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);

        byte[] found = IOUtils.toByteArray(lumifyApi.getVertexApi().getRaw(artifactVertexId));
        assertArrayEquals(expected, found);

        lumifyApi.logout();
    }

    private void assertThumbnailRoute() throws ApiException, IOException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);

        InputStream in = lumifyApi.getVertexApi().getThumbnail(artifactVertexId, 100);
        BufferedImage img = ImageIO.read(in);
        assertEquals(53, img.getWidth());
        assertEquals(100, img.getHeight());

        lumifyApi.logout();
    }

    private void resolveAndUnresolveDetectedObject() throws ApiException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);
        addUserAuths(lumifyApi, USERNAME_TEST_USER_1, "auth1");

        ClientApiDetectedObjects detectedObjects = lumifyApi.getVertexApi().getDetectedObjects(artifactVertexId, LumifyProperties.DETECTED_OBJECT.getPropertyName(), "");
        LOGGER.info("detectedObjects: %s", detectedObjects.toString());
        assertEquals(15, detectedObjects.getDetectedObjects().size());

        // Resolving a new detected object
        double x1 = 1.0;
        double x2 = 2.0;
        double y1 = 3.0;
        double y2 = 4.0;
        lumifyApi.getVertexApi().resolveDetectedObject(
                artifactVertexId,
                "Susan",
                CONCEPT_TEST_PERSON,
                "auth1",
                null,
                null,
                null,
                null,
                x1,
                x2,
                y1,
                y2
        );

        detectedObjects = lumifyApi.getVertexApi().getDetectedObjects(artifactVertexId, LumifyProperties.DETECTED_OBJECT.getPropertyName(), "");
        assertEquals(16, detectedObjects.getDetectedObjects().size());
        ClientApiProperty susanDetectedObject = findResolvedDetectedObject(detectedObjects, x1, x2, y1, y2);
        assertNotNull(susanDetectedObject);

        // Unresolving a detected object not found by opencv
        lumifyApi.getVertexApi().unresolveDetectedObject(artifactVertexId, susanDetectedObject.getKey());
        detectedObjects = lumifyApi.getVertexApi().getDetectedObjects(artifactVertexId, LumifyProperties.DETECTED_OBJECT.getPropertyName(), "");
        assertEquals(15, detectedObjects.getDetectedObjects().size());
        susanDetectedObject = findResolvedDetectedObject(detectedObjects, x1, x2, y1, y2);
        assertNull(susanDetectedObject);

        // Resolving a detected object that opencv found
        ClientApiDetectedObject testDetectedObjectValue = ClientApiDetectedObject.fromProperty(detectedObjects.getDetectedObjects().get(0));
        x1 = testDetectedObjectValue.getX1();
        x2 = testDetectedObjectValue.getX2();
        y1 = testDetectedObjectValue.getY1();
        y2 = testDetectedObjectValue.getY2();
        lumifyApi.getVertexApi().resolveDetectedObject(
                artifactVertexId,
                "Joe",
                CONCEPT_TEST_PERSON,
                "auth1",
                null,
                null,
                null,
                testDetectedObjectValue.getOriginalPropertyKey(),
                x1,
                x2,
                y1,
                y2
        );
        detectedObjects = lumifyApi.getVertexApi().getDetectedObjects(artifactVertexId, LumifyProperties.DETECTED_OBJECT.getPropertyName(), "");
        assertEquals(16, detectedObjects.getDetectedObjects().size());
        ClientApiProperty joeDetectedObject = findResolvedDetectedObject(detectedObjects, x1, x2, y1, y2);
        assertNotNull(joeDetectedObject);

        // Re-resolve a detected object
        lumifyApi.getVertexApi().resolveDetectedObject(
                artifactVertexId,
                "Jeff",
                CONCEPT_TEST_PERSON,
                "auth1",
                ClientApiDetectedObject.fromProperty(joeDetectedObject).getResolvedVertexId(),
                null,
                null,
                testDetectedObjectValue.getOriginalPropertyKey(),
                x1,
                x2,
                y1,
                y2
        );
        detectedObjects = lumifyApi.getVertexApi().getDetectedObjects(artifactVertexId, LumifyProperties.DETECTED_OBJECT.getPropertyName(), "");
        assertEquals(16, detectedObjects.getDetectedObjects().size());
        List<ClientApiProperty> jeffDetectedObjects = findResolvedDetectedObjects(detectedObjects, x1, x2, y1, y2);
        assertNotNull(jeffDetectedObjects);
        assertEquals(1, jeffDetectedObjects.size());

        // Unresolving a detected object that opencv found
        ClientApiProperty jeffDetectedObject = jeffDetectedObjects.get(0);
        lumifyApi.getVertexApi().unresolveDetectedObject(artifactVertexId, jeffDetectedObject.getKey());
        detectedObjects = lumifyApi.getVertexApi().getDetectedObjects(artifactVertexId, LumifyProperties.DETECTED_OBJECT.getPropertyName(), "");
        assertEquals(15, detectedObjects.getDetectedObjects().size());
        jeffDetectedObject = findResolvedDetectedObject(detectedObjects, x1, x2, y1, y2);
        assertNull(jeffDetectedObject);
    }

    private ClientApiProperty findResolvedDetectedObject(ClientApiDetectedObjects detectedObjects, double x1, double x2, double y1, double y2) {
        for (ClientApiProperty detectedObject : detectedObjects.getDetectedObjects()) {
            if (detectedObject.getValue() == null) {
                continue;
            }
            ClientApiDetectedObject detectedObjectValue = ClientApiDetectedObject.fromProperty(detectedObject);
            if (detectedObjectValue.getX1() == x1 &&
                    detectedObjectValue.getX2() == x2 &&
                    detectedObjectValue.getY1() == y1 &&
                    detectedObjectValue.getY2() == y2 &&
                    detectedObjectValue.getResolvedVertexId() != null) {
                return detectedObject;
            }
        }
        return null;
    }

    private List<ClientApiProperty> findResolvedDetectedObjects(ClientApiDetectedObjects detectedObjects, double x1, double x2, double y1, double y2) {
        List<ClientApiProperty> detectedObjectList = new ArrayList<ClientApiProperty>();
        for (ClientApiProperty detectedObject : detectedObjects.getDetectedObjects()) {
            if (detectedObject.getValue() == null) {
                continue;
            }
            ClientApiDetectedObject detectedObjectValue = ClientApiDetectedObject.fromProperty(detectedObject);
            if (detectedObjectValue.getX1() == x1 &&
                    detectedObjectValue.getX2() == x2 &&
                    detectedObjectValue.getY1() == y1 &&
                    detectedObjectValue.getY2() == y2 &&
                    detectedObjectValue.getResolvedVertexId() != null) {
                detectedObjectList.add(detectedObject);
            }
        }
        return detectedObjectList;
    }
}
