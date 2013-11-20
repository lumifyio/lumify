package com.altamiracorp.lumify.objectDetection;

import com.altamiracorp.lumify.core.ingest.ArtifactDetectedObject;
import com.altamiracorp.lumify.core.model.videoFrames.VideoFrameRepository;
import com.altamiracorp.lumify.core.model.artifact.ArtifactRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class OpenCVObjectDetectorTest {

    private static final String TEST_IMAGE = "cnn.jpg";
    private static final String CLASSIFIER = "haarcascade_frontalface_alt.xml";

    @Mock
    ArtifactRepository artifactRepository;

    @Mock
    VideoFrameRepository videoFrameRepository;

    @Before
    public void setUp() throws Exception {
        System.out.println(System.getProperty("java.library.path"));
    }

    @Test
    public void testObjectDetection() throws IOException {
        OpenCVObjectDetector objectDetector = new OpenCVObjectDetector(artifactRepository, videoFrameRepository);
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        BufferedImage bImage = ImageIO.read(cl.getResourceAsStream(TEST_IMAGE));

        objectDetector.setup(cl.getResource(CLASSIFIER).getPath());
        List<ArtifactDetectedObject> detectedObjectList = objectDetector.detectObjects(bImage);
        assertTrue("Incorrect number of objects found", detectedObjectList.size() == 1);

        ArtifactDetectedObject detectedObject = detectedObjectList.get(0);
        assertEquals("X1 incorrect", "434", detectedObject.getX1());
        assertEquals("Y1 incorrect", "117", detectedObject.getY1());
        assertEquals("X2 incorrect", "637", detectedObject.getX2());
        assertEquals("Y2 incorrect", "320", detectedObject.getY2());
    }

}
