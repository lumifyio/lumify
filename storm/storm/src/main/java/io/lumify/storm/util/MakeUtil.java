package io.lumify.storm.util;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.json.JSONObject;

public class MakeUtil {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(MakeUtil.class);

    public static String extractMakeFromJSON(JSONObject json) {
        if (json == null) {
            return null;
        }

        JSONObject formatObject = json.optJSONObject("format");
        if (formatObject != null) {
            JSONObject tagsObject = formatObject.optJSONObject("tags");
            if (tagsObject != null) {
                String deviceMake = tagsObject.optString("make");
                if (!deviceMake.equals("")) {
                    return deviceMake;
                }
                String deviceMakeEng = tagsObject.optString("make-eng");
                if (!deviceMakeEng.equals("")) {
                    return deviceMakeEng;
                }
            }
        }

        LOGGER.debug("Could not extract deviceMake from json.");
        return null;
    }
}
