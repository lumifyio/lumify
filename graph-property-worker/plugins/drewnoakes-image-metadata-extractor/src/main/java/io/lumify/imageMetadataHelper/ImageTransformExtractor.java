package io.lumify.imageMetadataHelper;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.imageMetadataExtractor.ImageTransform;

import java.io.File;
import java.io.IOException;

public class ImageTransformExtractor {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ImageTransformExtractor.class);

    public static ImageTransform getImageTransform(File localFile) {

        //new ImageTransform(false, 0) is the original image orientation, with no flip needed, and no rotation needed.
        ImageTransform imageTransform = new ImageTransform(false, 0);

        try {
            //Attempt to retrieve the metadata from the image.
            Metadata metadata = ImageMetadataReader.readMetadata(localFile);
            if (metadata != null) {
                ExifIFD0Directory exifDir = metadata.getDirectory(ExifIFD0Directory.class);
                if (exifDir != null) {
                    Integer orientationInteger = exifDir.getInteger(ExifIFD0Directory.TAG_ORIENTATION);
                    if (orientationInteger != null) {
                        imageTransform = convertOrientationToTransform(orientationInteger);
                    }
                }
            }
        } catch (ImageProcessingException e) {
            LOGGER.warn("drewnoakes metadata extractor threw ImageProcessingException when reading metadata." +
                    " Returning default orientation for image.");
        } catch (IOException e) {
            LOGGER.warn("drewnoakes metadata extractor threw IOException when reading metadata." +
                    " Returning default orientation for image.");
        }

        return imageTransform;
    }

    /**
     * Converts an orientation number to an ImageTransform object used by Lumify.
     *
     * @param orientationInt The EXIF orientation number, from 1 - 8, representing the combinations of 4 different
     *                       rotations and 2 different flipped values.
     * @return
     */
    public static ImageTransform convertOrientationToTransform(int orientationInt) {
        switch (orientationInt) {
            case 1:
                return new ImageTransform(false, 0);
            case 2:
                return new ImageTransform(true, 0);
            case 3:
                return new ImageTransform(false, 180);
            case 4:
                return new ImageTransform(true, 180);
            case 5:
                return new ImageTransform(true, 270);
            case 6:
                return new ImageTransform(false, 90);
            case 7:
                return new ImageTransform(true, 90);
            case 8:
                return new ImageTransform(false, 270);
            default:
                return new ImageTransform(false, 0);
        }
    }
}
