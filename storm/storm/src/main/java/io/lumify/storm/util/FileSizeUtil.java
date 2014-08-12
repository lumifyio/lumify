package io.lumify.storm.util;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.json.JSONObject;

public class FileSizeUtil {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(FileSizeUtil.class);

    public static Integer extractFileSizeFromJSON(JSONObject json) {
        if (json == null) {
            return null;
        }

        JSONObject formatObject = json.optJSONObject("format");
        if (formatObject != null) {
            Double optionalFileSize = formatObject.optDouble("size");
            if (!Double.isNaN(optionalFileSize)) {
                Integer fileSize = optionalFileSize.intValue();
                return fileSize;
            }
        }

        LOGGER.debug("Could not extract fileSize from json.");
        return null;
    }
}
