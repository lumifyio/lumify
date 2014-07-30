package io.lumify.imageMetadataHelper;

import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.GpsDirectory;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;

public class DirectionExtractor {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(DirectionExtractor.class);

    public static Double getImageFacingDirection(Metadata metadata) {
        GpsDirectory gpsDir = metadata.getDirectory(GpsDirectory.class);
        if (gpsDir != null) {
            //TODO. Assumes true direction for IMG_DIRECTION. Can check TAG_GPS_IMG_DIRECTION_REF to be more specific.
            try {
                Double imageFacingDirection = gpsDir.getDouble(GpsDirectory.TAG_GPS_IMG_DIRECTION);
                return imageFacingDirection;
            } catch (MetadataException e) {
                LOGGER.debug("getDouble(TAG_GPS_IMAGE_DIRECTION) threw MetadataException when attempting to" +
                        "retrieve GPS Image Direction.");
            }
        }
        return null;
    }

}
