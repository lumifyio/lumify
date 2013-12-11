package com.altamiracorp.lumify.core.model.videoFrames;

import com.altamiracorp.bigtable.model.Column;
import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.lumify.core.util.RowKeyHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class VideoFrameDetectedObjects extends ColumnFamily {

    public static final String NAME = "detected_objects";

    public VideoFrameDetectedObjects() {
        super(NAME);
    }

    public void addDetectedObject(String concept, String model, String x1, String y1, String x2, String y2) {
        String columnName = RowKeyHelper.buildMinor(concept, model, x1, y1, x2, y2);
        this.set(columnName, "");
    }

    @Override
    public JSONObject toJson() {
        try {
            JSONObject result = new JSONObject();
            JSONArray detectedObjects = new JSONArray();

            for (Column column : getColumns()) {
                JSONObject columnJson = new JSONObject();
                String[] parts = RowKeyHelper.splitOnMinorFieldSeperator(column.getName());
                columnJson.put("concept", parts[0]);
                columnJson.put("model", parts[1]);
                JSONObject coordsJson = new JSONObject();
                coordsJson.put("x1", parts[2]);
                coordsJson.put("y1", parts[3]);
                coordsJson.put("x2", parts[4]);
                coordsJson.put("y2", parts[5]);
                columnJson.put("coords", coordsJson);
                detectedObjects.put(columnJson);
            }

            result.put("detectedObjects", detectedObjects);
            return result;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
