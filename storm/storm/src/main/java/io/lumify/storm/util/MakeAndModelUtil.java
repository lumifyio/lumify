package io.lumify.storm.util;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.json.JSONObject;

public class MakeAndModelUtil {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(MakeAndModelUtil.class);


    public static String extractMakeFromJSON(JSONObject json) {
        return extractMakeAndModelFromJSON(json, "make", "make-eng");
    }

    public static String extractModelFromJSON(JSONObject json) {
        return extractMakeAndModelFromJSON(json, "model", "model-eng");
    }

    private static String extractMakeAndModelFromJSON(JSONObject json,
                                                      String firstSearchString,
                                                      String secondSearchString) {
        if (json == null) {
            return null;
        }

        JSONObject formatObject = json.optJSONObject("format");
        if (formatObject != null) {
            JSONObject tagsObject = formatObject.optJSONObject("tags");
            if (tagsObject != null) {
                String firstDesiredValue = tagsObject.optString(firstSearchString);
                if (!firstDesiredValue.equals("")) {
                    return firstDesiredValue;
                }
                String secondDesiredValue = tagsObject.optString(secondSearchString);
                if (!secondDesiredValue.equals("")) {
                    return secondDesiredValue;
                }
            }
        }

        LOGGER.debug("Could not extract " + firstSearchString + " or " + secondSearchString + "from json.");
        return null;
    }
}
