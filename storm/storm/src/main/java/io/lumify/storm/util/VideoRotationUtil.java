package io.lumify.storm.util;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.json.JSONArray;
import org.json.JSONObject;

public class VideoRotationUtil {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(VideoRotationUtil.class);

    public static Integer extractRotationFromJSON(JSONObject json) {
        if (json == null) {
            return null;
        }

        JSONArray streamsJson = json.optJSONArray("streams");
        if (streamsJson != null) {
            for (int i = 0; i < streamsJson.length(); i++) {
                JSONObject streamsIndexJson = streamsJson.optJSONObject(i);
                if (streamsIndexJson != null) {
                    JSONObject tagsJson = streamsIndexJson.optJSONObject("tags");
                    if (tagsJson != null) {
                        Integer rotate = tagsJson.optInt("rotate", 0) % 360;
                        return rotate;
                    }
                }
            }
        }

        LOGGER.debug("Could not retrieve a \"rotate\" value from the JSON object.");
        return null;
    }

    /**
     * Creates a String[] to pass as options for running the ffmpeg process.
     *
     * @return returns null when no rotation is needed for video.
     */
    public static String[] createFFMPEGRotationOptions(int videoRotation) {
        if (videoRotation % 360 == 90) {
            return new String[]{"-vf", "transpose=1"};
        } else if (videoRotation % 360 == 180) {
            return new String[]{"-vf", "transpose=1,transpose=1"};
        } else if (videoRotation % 360 == 270) {
            return new String[]{"-vf", "transpose=2"};
        } else {
            return null;
        }
    }

}
