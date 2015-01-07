package io.lumify.gpw.util;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.json.JSONArray;
import org.json.JSONObject;

public class FFprobeDimensionsUtil {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(FFprobeDimensionsUtil.class);

    public static Integer getWidth(JSONObject json) {
        Integer width = getDimension(json, "width");
        if (width == null) {
            LOGGER.debug("Could not retrieve a \"width\" value from the JSON object.");
        }
        return width;
    }

    public static Integer getHeight(JSONObject json) {
        Integer height = getDimension(json, "height");
        if (height == null) {
            LOGGER.debug("Could not retrieve a \"height\" value from the JSON object.");
        }
        return height;
    }

    private static Integer getDimension(JSONObject json, String dimension) {
        if (json == null) {
            return null;
        }

        JSONArray streamsJson = json.optJSONArray("streams");
        if (streamsJson != null) {
            for (int i = 0; i < streamsJson.length(); i++) {
                JSONObject streamsIndexJson = streamsJson.optJSONObject(i);
                if (streamsIndexJson != null) {
                    Double optionalDimension = streamsIndexJson.optDouble(dimension);
                    if (!Double.isNaN(optionalDimension)) {
                        return optionalDimension.intValue();
                    }
                }
            }
        }

        return null;
    }
}
