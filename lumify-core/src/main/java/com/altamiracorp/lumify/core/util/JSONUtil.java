package com.altamiracorp.lumify.core.util;

import org.json.JSONArray;
import org.json.JSONObject;

public class JSONUtil {
    public static JSONArray getOrCreateJSONArray(JSONObject json, String name) {
        JSONArray arr = json.optJSONArray(name);
        if (arr == null) {
            arr = new JSONArray();
            json.put(name, arr);
        }
        return arr;
    }

    public static void addToJSONArrayIfDoesNotExist(JSONArray jsonArray, Object value) {
        if (!arrayConains(jsonArray, value)) {
            jsonArray.put(value);
        }
    }

    public static boolean arrayConains(JSONArray jsonArray, Object value) {
        for (int i = 0; i < jsonArray.length(); i++) {
            if (jsonArray.get(i).equals(value)) {
                return true;
            }
        }
        return false;
    }
}
