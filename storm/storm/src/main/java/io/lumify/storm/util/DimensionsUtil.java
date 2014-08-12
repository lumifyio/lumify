package io.lumify.storm.util;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.json.JSONArray;
import org.json.JSONObject;

public class DimensionsUtil {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(DimensionsUtil.class);

    public static Integer extractWidthFromJSON(JSONObject json) {
        Integer width = extractDimensionFromJSON(json, "width");
        if (width == null) {
            LOGGER.debug("Could not retrieve a \"width\" value from the JSON object.");
        }
        return width;
    }

    public static Integer extractHeightFromJSON(JSONObject json) {
        Integer height = extractDimensionFromJSON(json, "height");
        if (height == null) {
            LOGGER.debug("Could not retrieve a \"height\" value from the JSON object.");
        }
        return height;
    }

    private static Integer extractDimensionFromJSON(JSONObject json, String dimensionToRetrieve) {
        if (json == null) {
            return null;
        }

        if (dimensionToRetrieve == null) {
            return null;
        } else if (dimensionToRetrieve.equals("width") || dimensionToRetrieve.equals("height")) {

            JSONArray streamsJson = json.optJSONArray("streams");
            if (streamsJson != null) {
                for (int i = 0; i < streamsJson.length(); i++) {
                    JSONObject streamsIndexJson = streamsJson.optJSONObject(i);
                    if (streamsIndexJson != null) {
                        Double optionalDimension = streamsIndexJson.optDouble(dimensionToRetrieve);
                        if (!Double.isNaN(optionalDimension)) {
                            Integer dimension = optionalDimension.intValue();
                            return dimension;
                        }
                    }
                }
            }
        }

        return null;
    }

}
