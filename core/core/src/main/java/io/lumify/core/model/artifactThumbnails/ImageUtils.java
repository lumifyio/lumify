package io.lumify.core.model.artifactThumbnails;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageUtils {


    /**
     * Flipping (Mirroring) is performed BEFORE Rotating the image. Example: Flipping the image over the y Axis, and then rotating it 90 degrees CW
     * is not the same as rotating the image 90 degrees CW and then flipping it over the y Axis.
     */
    public static BufferedImage reOrientImage(BufferedImage image, boolean yAxisFlipNeeded, int cwRotationNeeded) {
        //If angle greater than 360 is entered, reduce this angle.
        cwRotationNeeded = cwRotationNeeded % 360;

        BufferedImage orientedImage = image;
        if (!yAxisFlipNeeded && cwRotationNeeded == 0) {
            //EXIF Orientation 1.
            return image;
        } else if (yAxisFlipNeeded && cwRotationNeeded == 0) {
            //EXIF Orientation 2.
            orientedImage = flipImageHorizontally(image);
        } else if (!yAxisFlipNeeded && cwRotationNeeded == 180) {
            //EXIF Orientation 3.
            orientedImage = rotateImage(image, 180);
        } else if (yAxisFlipNeeded && cwRotationNeeded == 180) {
            //EXIF Orientation 4.
            orientedImage = flipImageVertically(image);
        } else if (yAxisFlipNeeded && cwRotationNeeded == 270) {
            //EXIF Orientation 5.
            orientedImage = flipImageVertically(image);
            orientedImage = rotateImage(orientedImage, 90);
        } else if (!yAxisFlipNeeded && cwRotationNeeded == 90) {
            //EXIF Orientation 6.
            orientedImage = rotateImage(image, 90);
        } else if (yAxisFlipNeeded && cwRotationNeeded == 90) {
            //EXIF Orientation 7.
            orientedImage = flipImageVertically(image);
            orientedImage = rotateImage(orientedImage, 270);
        } else if (!yAxisFlipNeeded && cwRotationNeeded == 270) {
            //EXIF Orientation 8.
            orientedImage = rotateImage(image, 270);
        } else {
            return image;
        }

        return orientedImage;
    }

    public static BufferedImage flipImageHorizontally(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int type = thumbnailType(image);
        BufferedImage result = new BufferedImage(width, height, type);
        Graphics2D g = result.createGraphics();
        g.drawImage(image, width, 0, 0, height, 0, 0, width, height, null);
        g.dispose();
        return result;
    }

    public static BufferedImage flipImageVertically(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int type = thumbnailType(image);
        BufferedImage result = new BufferedImage(width, height, type);
        Graphics2D g = result.createGraphics();
        g.drawImage(image, 0, height, width, 0, 0, 0, width, height, null);
        g.dispose();
        return result;
    }


    public static BufferedImage rotateImage(BufferedImage image,
                                            int cwRotationNeeded) {
        double angle = Math.toRadians(cwRotationNeeded);
        int type = thumbnailType(image);
        double sin = Math.abs(Math.sin(angle));
        double cos = Math.abs(Math.cos(angle));
        int width = image.getWidth();
        int height = image.getHeight();
        int newWidth = (int) Math.floor(width * cos + height * sin);
        int newHeight = (int) Math.floor(height * cos + width * sin);
        BufferedImage result = new BufferedImage(newWidth, newHeight, type);
        Graphics2D g = result.createGraphics();
        g.translate((newWidth - width) / 2, (newHeight - height) / 2);
        g.rotate(angle, width / 2, height / 2);
        g.drawRenderedImage(image, null);
        g.dispose();
        return result;
    }

    public static int thumbnailType(BufferedImage image) {
        if (image.getColorModel().getNumComponents() > 3) {
            return BufferedImage.TYPE_4BYTE_ABGR;
        } else if (image.getColorModel().getNumColorComponents() == 3) {
            return BufferedImage.TYPE_3BYTE_BGR;
        }
        return BufferedImage.TYPE_INT_RGB;
    }

    public static String thumbnailFormat(BufferedImage image) {
        if (image.getColorModel().getNumComponents() > 3) {
            return "png";
        }
        return "jpg";
    }
}
