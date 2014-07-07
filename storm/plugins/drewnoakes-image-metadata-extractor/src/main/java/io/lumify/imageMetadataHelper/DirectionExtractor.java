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

    public static String getImageFacingDirectionDescription(Metadata metadata) {
        Double imageFacingDirection = getImageFacingDirection(metadata);
        if (imageFacingDirection != null) {
            String directionString = convertDegreeToDirection(imageFacingDirection);
            if (directionString != null) {
                String imageDirectionString = directionString + " (" + Math.round(imageFacingDirection) + "°)";
                return imageDirectionString;
            }
        }
        return null;
    }

    private static String convertDegreeToDirection(double degree) {
        if (degree > 22.5 && degree <= 67.5) {
            return "NE";
        } else if (degree > 67.5 && degree <= 112.5) {
            return "E";
        } else if (degree > 112.5 && degree <= 157.5) {
            return "SE";
        } else if (degree > 157.5 && degree <= 202.5) {
            return "S";
        } else if (degree > 202.5 && degree <= 247.5) {
            return "SW";
        } else if (degree > 247.5 && degree <= 292.5) {
            return "W";
        } else if (degree > 292.5 && degree <= 337.5) {
            return "NW";
        } else if ((degree > 337.5 && degree <= 360)
                || (degree >= 0 && degree <= 22.5)) {
            return "N";
        } else {
            //because improper degree. Negative degree.
            return null;
        }
    }
}
