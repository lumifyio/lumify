package com.altamiracorp.lumify.core.model.detectedObjects;

import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.bigtable.model.RowKey;
import com.altamiracorp.lumify.core.model.properties.LumifyProperties;
import org.json.JSONException;
import org.json.JSONObject;

public class DetectedObjectModel extends Row<DetectedObjectRowKey> {
    public static final String TABLE_NAME = "atc_detectedObject";

    public DetectedObjectModel(DetectedObjectRowKey rowKey) {
        super(TABLE_NAME, rowKey);
    }

    public DetectedObjectModel() {
        super(TABLE_NAME);
    }

    public DetectedObjectModel(RowKey rowKey) {
        this(new DetectedObjectRowKey(rowKey.toString()));
    }

    public DetectedObjectMetadata getMetadata() {
        DetectedObjectMetadata detectedObjectMetadata = get(DetectedObjectMetadata.NAME);
        if (detectedObjectMetadata == null) {
            addColumnFamily(new DetectedObjectMetadata());
        }
        return get(DetectedObjectMetadata.NAME);
    }

    public JSONObject toJson() {
        try {
            JSONObject json = new JSONObject();
            if (getMetadata().getResolvedId() == null) {
                json.put("classifierConcept", getMetadata().getClassiferConcept());
            }
            json.put(LumifyProperties.ROW_KEY.getKey(), getRowKey());
            json.put("x1", getMetadata().getX1());
            json.put("y1", getMetadata().getY1());
            json.put("x2", getMetadata().getX2());
            json.put("y2", getMetadata().getY2());
            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
