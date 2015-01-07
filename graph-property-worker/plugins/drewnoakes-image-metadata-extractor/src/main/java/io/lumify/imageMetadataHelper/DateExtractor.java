package io.lumify.imageMetadataHelper;

import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import java.util.Date;

public class DateExtractor {

    /**
     * Checks the metadata directories in order until the date is found. The first match is returned.
     * <p/>
     * NOTE: Only the ExifIFD0Directory and ExifSubIFDDirectory directories will be scanned for dates. The other 8
     * directories will not be scanned for date and time information yet. (Perhaps implement later).
     */
    public static Date getDateDefault(Metadata metadata) {

        Date date = null;

        ExifIFD0Directory exifDir = metadata.getDirectory(ExifIFD0Directory.class);
        if (exifDir != null) {
            date = exifDir.getDate(ExifIFD0Directory.TAG_DATETIME);
            if (date != null) {
                return date;
            }
        }

        ExifSubIFDDirectory subDir = metadata.getDirectory(ExifSubIFDDirectory.class);
        if (subDir != null) {
            date = subDir.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
            if (date != null) {
                return date;
            }
        }

        return null;
    }

}
