package io.lumify.core.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.exception.LumifyJsonParseException;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.web.clientapi.model.util.ObjectMapperFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JSONUtil {
    private static ObjectMapper mapper = ObjectMapperFactory.getInstance();

    public static JSONArray getOrCreateJSONArray(JSONObject json, String name) {
        JSONArray arr = json.optJSONArray(name);
        if (arr == null) {
            arr = new JSONArray();
            json.put(name, arr);
        }
        return arr;
    }

    public static void addToJSONArrayIfDoesNotExist(JSONArray jsonArray, Object value) {
        if (!arrayContains(jsonArray, value)) {
            jsonArray.put(value);
        }
    }

    public static int arrayIndexOf(JSONArray jsonArray, Object value) {
        for (int i = 0; i < jsonArray.length(); i++) {
            if (jsonArray.get(i).equals(value)) {
                return i;
            }
        }
        return -1;
    }

    public static boolean arrayContains(JSONArray jsonArray, Object value) {
        return arrayIndexOf(jsonArray, value) != -1;
    }

    public static void removeFromJSONArray(JSONArray jsonArray, Object value) {
        int idx = arrayIndexOf(jsonArray, value);
        if (idx >= 0) {
            jsonArray.remove(idx);
        }
    }

    public static JSONObject parse(String jsonString) {
        try {
            return new JSONObject(jsonString);
        } catch (JSONException ex) {
            throw new LumifyJsonParseException(jsonString, ex);
        }
    }

    public static JSONArray parseArray(String s) {
        try {
            return new JSONArray(s);
        } catch (JSONException ex) {
            throw new LumifyJsonParseException(s, ex);
        }
    }

    public static JsonNode toJsonNode(JSONObject json) {
        try {
            if (json == null) {
                return null;
            }
            return mapper.readTree(json.toString());
        } catch (IOException e) {
            throw new LumifyException("Could not create json node from: " + json.toString(), e);
        }
    }

    public static Map<String, String> toMap(JSONObject json) {
        Map<String, String> results = new HashMap<String, String>();
        for (Object key : json.keySet()) {
            String keyStr = (String) key;
            results.put(keyStr, json.getString(keyStr));
        }
        return results;
    }

    public static List<String> toStringList(JSONArray arr) {
        List<String> result = new ArrayList<String>();
        for (int i = 0; i < arr.length(); i++) {
            result.add(arr.getString(i));
        }
        return result;
    }

    public static JSONObject toJson(Map<String, String> map) {
        JSONObject json = new JSONObject();
        for (Map.Entry<String, String> e : map.entrySet()) {
            json.put(e.getKey(), e.getValue());
        }
        return json;
    }
}
