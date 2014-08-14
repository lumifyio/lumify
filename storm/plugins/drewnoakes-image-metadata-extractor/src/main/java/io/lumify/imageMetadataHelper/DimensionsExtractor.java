package io.lumify.imageMetadataHelper;

import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.jpeg.JpegDirectory;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class DimensionsExtractor {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(DimensionsExtractor.class);

    private static enum Dimension {
        WIDTH, HEIGHT
    }

    /**
     * Checks the metadata directories in order until the width is found. The first match found in a directory
     * is returned.
     *
     * @param metadata
     * @return
     */
    public static Integer getWidthViaMetadata(Metadata metadata) {
        return getDimensionViaMetadata(metadata, Dimension.WIDTH);
    }

    /**
     * Checks the metadata directories in order until the height is found. The first match found in a directory
     * is returned.
     *
     * @param metadata
     * @return
     */
    public static Integer getHeightViaMetadata(Metadata metadata) {
        return getDimensionViaMetadata(metadata, Dimension.HEIGHT);
    }


    private static Integer getDimensionViaMetadata(Metadata metadata, DimensionsExtractor.Dimension dimensionType) {
        if (dimensionType == null ){
            return null;
        }

        int exifDimensionTag;
        int jpegDimensionTag;
        if (dimensionType == Dimension.WIDTH){
            exifDimensionTag = ExifSubIFDDirectory.TAG_EXIF_IMAGE_WIDTH;
            jpegDimensionTag = JpegDirectory.TAG_JPEG_IMAGE_WIDTH;
        }
        else if (dimensionType == Dimension.HEIGHT){
            exifDimensionTag = ExifSubIFDDirectory.TAG_EXIF_IMAGE_HEIGHT;
            jpegDimensionTag = JpegDirectory.TAG_JPEG_IMAGE_HEIGHT;
        } else {
            throw new IllegalArgumentException("dimensionType was not equal to 'HEIGHT' or 'WIDTH'.");
        }

        ExifSubIFDDirectory exifDir = metadata.getDirectory(ExifSubIFDDirectory.class);
        if (exifDir != null) {
                Integer dimension = exifDir.getInteger(exifDimensionTag);
            if (dimension != null && !dimension.equals(0)) {
                return dimension;
            }
        }

        JpegDirectory jpegDir = metadata.getDirectory(JpegDirectory.class);
        if (jpegDir != null) {
            Integer dimension = jpegDir.getInteger(jpegDimensionTag);
            if (dimension != null && !dimension.equals(0)) {
                return dimension;
            }
        }

        return null;
    }

    /**
     * Get the width of the image file by loading the file as a buffered image.
     * @return
     */
    public static Integer getWidthViaBufferedImage(File imageFile){
        try {
            BufferedImage bufImage = ImageIO.read(imageFile);
            int width = bufImage.getWidth();
            return width;
        } catch (IOException e){
            if (imageFile != null) {
                LOGGER.debug("Could not read imageFile: " + imageFile.getName());
            }
        }
        return null;
    }

    /**
     * Get the height of the image file by loading the file as a buffered image.
     * @return
     */
    public static Integer getHeightViaBufferedImage(File imageFile){
        try {
            BufferedImage bufImage = ImageIO.read(imageFile);
            int height = bufImage.getHeight();
            return height;
        } catch (IOException e){
            if (imageFile != null) {
                LOGGER.debug("Could not read imageFile: " + imageFile.getName());
            }
        }
        return null;
    }

}
