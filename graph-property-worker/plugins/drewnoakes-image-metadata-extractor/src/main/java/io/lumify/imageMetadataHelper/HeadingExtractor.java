package io.lumify.imageMetadataHelper;

import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.GpsDirectory;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;

public class HeadingExtractor {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(HeadingExtractor.class);

    public static Double getImageHeading(Metadata metadata) {
        GpsDirectory gpsDir = metadata.getDirectory(GpsDirectory.class);
        if (gpsDir != null) {
            //TODO. Assumes true direction for IMG_DIRECTION. Can check TAG_GPS_IMG_DIRECTION_REF to be more specific.
            try {
                Double imageHeading = gpsDir.getDouble(GpsDirectory.TAG_GPS_IMG_DIRECTION);
                return imageHeading;
            } catch (MetadataException e) {
                LOGGER.debug("getDouble(TAG_GPS_IMAGE_DIRECTION) threw MetadataException when attempting to" +
                        "retrieve GPS Heading.");
            }
        }
        return null;
    }

}
