package io.lumify.core.model.artifactThumbnails;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageUtils {

    public static BufferedImage rotateAndFlipBufferedImage(BufferedImage image,
                                                           Integer rotationNeeded) {
        if (rotationNeeded == null){
            rotationNeeded = 0;
        }
        double angle = Math.toRadians(rotationNeeded);
        int type = thumbnailType(image);
        double sin = Math.abs(Math.sin(angle)), cos = Math.abs(Math.cos(angle));
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
