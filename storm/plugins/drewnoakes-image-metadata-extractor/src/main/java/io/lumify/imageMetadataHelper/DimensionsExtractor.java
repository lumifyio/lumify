package io.lumify.imageMetadataHelper;

import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.jpeg.JpegDirectory;

public class DimensionsExtractor {
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
    public static Integer getWidth(Metadata metadata) {
        return getDimension(metadata, Dimension.WIDTH);
    }

    /**
     * Checks the metadata directories in order until the height is found. The first match found in a directory
     * is returned.
     *
     * @param metadata
     * @return
     */
    public static Integer getHeight(Metadata metadata) {
        return getDimension(metadata, Dimension.HEIGHT);
    }


    private static Integer getDimension(Metadata metadata, DimensionsExtractor.Dimension dimensionType) {
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
}
