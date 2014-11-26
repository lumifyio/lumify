package io.lumify.opencvObjectDetector;

import org.opencv.core.Core;
import org.opencv.core.Mat;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.opencv.core.CvType.*;

public class OpenCVUtils {

    public static Mat bufferedImageToMat(BufferedImage image) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        if (image != null) {
            Mat mat;
            int numComponents = image.getColorModel().getNumComponents();
            switch (numComponents) {
                case 1:
                    mat = new Mat(image.getHeight(), image.getWidth(), CV_8UC1);
                    break;
                case 2:
                    mat = new Mat(image.getHeight(), image.getWidth(), CV_8UC2);
                    break;
                case 3:
                    mat = new Mat(image.getHeight(), image.getWidth(), CV_8UC3);
                    break;
                case 4:
                    mat = new Mat(image.getHeight(), image.getWidth(), CV_8UC4);
                    break;
                default:
                    throw new RuntimeException("Image has an unsupportable number of channels: " + numComponents);
            }

            if (image.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
                image = convertByteIndexedImage(image);
            }

            byte[] pixelData = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
            mat.put(0, 0, pixelData);

            Mat mat3;
            if (numComponents == 3) {
                mat3 = mat;
            } else {
                mat3 = new Mat(image.getHeight(), image.getWidth(), CV_8UC3);
                mat.convertTo(mat3, CV_8UC3);
            }

            return mat3;
        }
        return null;
    }

    public static BufferedImage matToBufferedImage(Mat mat) throws IOException {
        byte[] pixelData = new byte[(int) (mat.total() * mat.channels())];
        mat.get(0, 0, pixelData);

        return ImageIO.read(new ByteArrayInputStream(pixelData));
    }

    private static BufferedImage convertByteIndexedImage(BufferedImage image) {
        BufferedImage convertedBImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g2 = convertedBImage.createGraphics();
        g2.drawImage(image, 0, 0, null);
        g2.dispose();
        return convertedBImage;
    }
}
