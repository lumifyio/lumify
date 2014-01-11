package com.altamiracorp.lumify.model.accumuloqueue.model;

import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.bigtable.model.Value;
import org.json.JSONArray;
import org.json.JSONObject;

public class QueueItem extends Row<QueueItemRowKey> {
    public static final String COLUMN_FAMILY_NAME = "";
    public static final String COLUMN_NAME = "";

    public QueueItem(String tableName, QueueItemRowKey rowKey) {
        super(tableName, rowKey);
    }

    public QueueItem(String tableName) {
        super(tableName);
    }

    public QueueItem(String tableName, JSONObject json, String[] extra) {
        super(tableName, new QueueItemRowKey(System.nanoTime()));

        JSONObject jsonWithExtras = createJsonWithExtras(json, extra);
        ColumnFamily cf = new ColumnFamily(COLUMN_FAMILY_NAME);
        cf.set(COLUMN_NAME, jsonWithExtras.toString());
        addColumnFamily(cf);
    }

    private static JSONObject createJsonWithExtras(JSONObject json, String[] extra) {
        JSONObject result = new JSONObject();
        result.put("json", json);
        if (extra.length > 0) {
            JSONArray extraJson = new JSONArray();
            for (String e : extra) {
                extraJson.put(e);
            }
            result.put("extra", extraJson);
        }
        return result;
    }

    public JSONObject getJson() {
        String jsonString = Value.toString(get(COLUMN_FAMILY_NAME).get(COLUMN_NAME));
        JSONObject json = new JSONObject(jsonString);
        return json.getJSONObject("json");
    }
}
