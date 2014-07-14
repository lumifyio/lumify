package io.lumify.storm.video;

import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.core.util.ProcessRunner;
import io.lumify.storm.JSONExtractor;
import org.json.JSONArray;
import org.json.JSONObject;

public class VideoRotation {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(VideoRotation.class);

    public static Integer retrieveVideoRotation(ProcessRunner processRunner, GraphPropertyWorkData data) {
        JSONObject json;
        try {
            json = JSONExtractor.retrieveJSONObjectUsingFFPROBE(processRunner, data);
        } catch (Exception e) {
            return null;
        }

        if (json != null) {
            Integer videoRotation = extractRotationFromMovAndMp4JSON(json);
            return videoRotation;
        } else {
            return null;
        }
    }

    private static Integer extractRotationFromMovAndMp4JSON(JSONObject json) {
        Integer rotate = null;
        try {
            JSONArray streamsJson = json.optJSONArray("streams");
            JSONObject streamsIndex0Json = streamsJson.optJSONObject(0);
            JSONObject tagsJson = streamsIndex0Json.optJSONObject("tags");
            rotate = tagsJson.optInt("rotate");
        } catch (NullPointerException e) {
            LOGGER.info("Could not retrieve a \"rotate\" value from the JSON object.");
        }
        return rotate;
    }

    /**
     * Creates a String[] to pass as options for running the ffmpeg process.
     *
     * @return returns null when no rotation is needed for video.
     */
    public static String[] createFFMPEGRotationOptions(int videoRotation) {
        if (videoRotation == 90) {
            return new String[]{"-vf", "transpose=1"};
        } else if (videoRotation == 180) {
            return new String[]{"-vf", "transpose=1,transpose=1"};
        } else if (videoRotation == 270) {
            return new String[]{"-vf", "transpose=2"};
        } else {
            return null;
        }
    }

}
