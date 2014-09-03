package io.lumify.imageMetadataHelper;

import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.xmp.XmpDirectory;

public class ModelExtractor {

    /**
     * Checks the metadata directories in order until the model is found. The first match found in a directory
     * is returned.
     *
     * @param metadata
     * @return
     */
    public static String getModel(Metadata metadata) {

        String modelString = null;

        ExifIFD0Directory exifDir = metadata.getDirectory(ExifIFD0Directory.class);
        if (exifDir != null) {
            modelString = exifDir.getDescription(ExifIFD0Directory.TAG_MODEL);
            if (modelString != null && !modelString.equals("none")) {
                return modelString;
            }
        }

        XmpDirectory xmpDir = metadata.getDirectory(XmpDirectory.class);
        if (modelString != null && !modelString.equals("none")) {
            modelString = xmpDir.getDescription(XmpDirectory.TAG_MODEL);
            if (modelString != null) {
                return modelString;
            }
        }
        return null;
    }
}
