package io.lumify.opencvObjectDetector;

import io.lumify.core.ingest.ArtifactDetectedObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.opencv.objdetect.CascadeClassifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class OpenCVObjectDetectorPropertyWorkerTest {
    private static final String TEST_IMAGE = "cnn.jpg";
    private static final String CLASSIFIER = "haarcascade_frontalface_alt.xml";

    @Before
    public void setUp() throws Exception {
        System.out.println(System.getProperty("java.library.path"));
    }

    @Test
    public void testObjectDetection() throws Exception {
        OpenCVObjectDetectorPropertyWorker objectDetector = new OpenCVObjectDetectorPropertyWorker();
        objectDetector.loadNativeLibrary();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        BufferedImage bImage = ImageIO.read(cl.getResourceAsStream(TEST_IMAGE));

        CascadeClassifier objectClassifier = new CascadeClassifier(cl.getResource(CLASSIFIER).getPath());
        objectDetector.addObjectClassifier("face", objectClassifier, "http://test.lumify.io/#face");
        List<ArtifactDetectedObject> detectedObjectList = objectDetector.detectObjects(bImage);
        assertTrue("Incorrect number of objects found", detectedObjectList.size() == 1);

        ArtifactDetectedObject detectedObject = detectedObjectList.get(0);
        assertEquals("http://test.lumify.io/#face", detectedObject.getConcept());
        assertEquals(0.423828125, detectedObject.getX1(), 0.0);
        assertEquals(0.1828125, detectedObject.getY1(), 0.0);
        assertEquals(0.6220703125, detectedObject.getX2(), 0.0);
        assertEquals(0.5, detectedObject.getY2(), 0.0);
    }
}
