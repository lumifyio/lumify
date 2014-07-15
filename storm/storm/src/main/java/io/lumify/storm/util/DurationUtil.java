package io.lumify.storm.util;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.json.JSONObject;

public class DurationUtil {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(DurationUtil.class);

    public static Double extractDurationFromJSON(JSONObject json){
        if (json == null)
            return null;

        Double duration = null;
        try {
            JSONObject formatJson = json.optJSONObject("format");
            duration = formatJson.optDouble("duration");
        } catch (Exception e){
            LOGGER.info("Could not retrieve a \"duration\" value from the JSON object.");
        }
        return duration;
    }
}
