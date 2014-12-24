package io.lumify.opencvObjectDetector;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.opencv.core.Mat;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class OpenCVUtilsTest {

    private static final String TEST_4_CHANNEL_IMAGE = "colbert-4channel.png";
    private static final String TEST_3_CHANNEL_IMAGE = "colbert-3channel.png";
    private static final String TEST_GRAYSCALE_IMAGE = "colbert-gray.png";

    @Before
    public void setUp() throws Exception {
        System.out.println(System.getProperty("java.library.path"));
    }

    @Test
    public void testBufferedImageToMat() throws IOException {
        Mat mat = matForImage(TEST_4_CHANNEL_IMAGE, 4);
        assertEquals(4, mat.channels());

        mat = matForImage(TEST_3_CHANNEL_IMAGE, 3);
        assertEquals(3, mat.channels());

        mat = matForImage(TEST_GRAYSCALE_IMAGE, 1);
        assertEquals(1, mat.channels());
    }


    private Mat matForImage(String imageName, int expectedComponents) throws IOException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        BufferedImage bImage = ImageIO.read(cl.getResourceAsStream(imageName));

        assertEquals(expectedComponents, bImage.getColorModel().getNumComponents());

        return OpenCVUtils.bufferedImageToMat(bImage);
    }
}
