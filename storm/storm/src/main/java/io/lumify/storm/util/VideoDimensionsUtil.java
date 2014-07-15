package io.lumify.storm.util;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.json.JSONArray;
import org.json.JSONObject;

public class VideoDimensionsUtil {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(VideoDimensionsUtil.class);

    public static Integer extractWidthFromJSON(JSONObject json) {
        if (json == null)
            return null;

        Integer width = null;
        try {
            JSONArray streamsJson = json.optJSONArray("streams");
            JSONObject streamsIndex0Json = streamsJson.optJSONObject(0);
            width = streamsIndex0Json.optInt("width");
        } catch (Exception e) {
            LOGGER.info("Could not retrieve a \"width\" value from the JSON object.");
        }
        return width;
    }

    public static Integer extractHeightFromJSON(JSONObject json) {
        if (json == null)
            return null;

        Integer height = null;
        try {
            JSONArray streamsJson = json.optJSONArray("streams");
            JSONObject streamsIndex0Json = streamsJson.optJSONObject(0);
            height = streamsIndex0Json.optInt("height");
        } catch (Exception e) {
            LOGGER.info("Could not retrieve a \"height\" value from the JSON object.");
        }
        return height;
    }

    public static String createFFMPEGScaleOptions(Integer videoWidth, Integer videoHeight, Integer videoRotation) {
        //Assumed display rectangle to fit into = 720:480.
        int maxWidth = 720;
        int maxHeight = 480;

        if (videoWidth == null || videoHeight == null) {
            return "scale=" + maxWidth + ":" + maxHeight;
        }
        if (videoRotation == null)
            videoRotation = 0;

        //Switch width and height if a rotation will occur.
        if (videoRotation % 360 == 90 || videoRotation % 360 == 270) {
            int temp = videoWidth;
            videoWidth = videoHeight;
            videoHeight = temp;
        }

        //Let the video be no larger than maxWidth:maxHeight.
        double aspectRatio = (double) videoWidth / videoHeight;
        int newWidth, newHeight;
        if (videoHeight > videoWidth) {
            newHeight = maxHeight;
            newWidth = (int) Math.round(newHeight * aspectRatio);
        } else {
            newWidth = maxWidth;
            newHeight = (int) Math.round(newWidth / aspectRatio);
        }

        return "scale=" + newWidth + ":" + newHeight;
    }


}
