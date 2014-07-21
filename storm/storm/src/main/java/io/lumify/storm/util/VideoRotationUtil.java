package io.lumify.storm.util;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class VideoRotationUtil {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(VideoRotationUtil.class);

    public static Integer extractRotationFromJSON(JSONObject json) {
        if (json == null) {
            return null;
        }

        try {
            JSONArray streamsJson = json.getJSONArray("streams");
            for (int i = 0; i < streamsJson.length(); i++) {
                try {
                    JSONObject streamsIndexJson = streamsJson.getJSONObject(i);
                    JSONObject tagsJson = streamsIndexJson.getJSONObject("tags");
                    Integer rotate = tagsJson.getInt("rotate") % 360;
                    return rotate;
                } catch (JSONException e) {
                    //Could not find "rotate" name on this pathway. No action needed.
                }
            }
        } catch (Exception e) {
            LOGGER.info("Could not retrieve a \"rotate\" value from the JSON object.");
        }
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
