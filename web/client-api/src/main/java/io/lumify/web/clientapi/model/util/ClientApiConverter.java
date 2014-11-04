package io.lumify.web.clientapi.model.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.lumify.web.clientapi.model.VisibilityJson;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;

public class ClientApiConverter {
    public static Object toClientApiValue(Object value) {
        if (value instanceof JSONArray) {
            JSONArray json = (JSONArray) value;
            List<Object> result = new ArrayList<Object>();
            for (int i = 0; i < json.length(); i++) {
                Object obj = json.get(i);
                result.add(toClientApiValue(obj));
            }
            return result;
        } else if (value instanceof JSONObject) {
            JSONObject json = (JSONObject) value;
            if (json.length() == 2 && json.has("source") && json.has("workspaces")) {
                VisibilityJson visibilityJson = new VisibilityJson();
                visibilityJson.setSource(json.getString("source"));
                JSONArray workspacesJson = json.getJSONArray("workspaces");
                for (int i = 0; i < workspacesJson.length(); i++) {
                    visibilityJson.addWorkspace(workspacesJson.getString(i));
                }
                return visibilityJson;
            }
            Map<String, Object> result = new HashMap<String, Object>();
            for (Object key : json.keySet()) {
                String keyStr = (String) key;
                result.put(keyStr, toClientApiValue(json.get(keyStr)));
            }
            return result;
        } else if (value instanceof String) {
            try {
                String valueString = (String) value;
                valueString = valueString.trim();
                if (valueString.startsWith("{") && valueString.endsWith("}")) {
                    return toClientApiValue(new JSONObject(valueString));
                }
            } catch (Exception ex) {
                // ignore this exception it just mean the string wasn't really json
            }
        } else if (value instanceof Date) {
            return toClientApiValue(((Date) value).getTime());
        }
        return value;
    }

    public static Object fromClientApiValue(Object obj) {
        if (obj instanceof Map) {
            Map map = (Map) obj;
            if (map.size() == 2 && map.containsKey("source") && map.containsKey("workspaces")) {
                VisibilityJson visibilityJson = new VisibilityJson();
                visibilityJson.setSource((String) map.get("source"));
                List<String> workspaces = (List<String>) map.get("workspaces");
                for (String workspace : workspaces) {
                    visibilityJson.addWorkspace(workspace);
                }
                return visibilityJson;
            }
        }
        return obj;
    }

    public static String clientApiToString(Object o) {
        if (o == null) {
            throw new RuntimeException("o cannot be null.");
        }
        try {
            return ObjectMapperFactory.getInstance().writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not convert object '" + o.getClass().getName() + "' to string", e);
        }
    }

    public static <T> T toClientApi(String str, Class<T> clazz) {
        try {
            return ObjectMapperFactory.getInstance().readValue(str, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Could not parse '" + str + "' to class '" + clazz.getName() + "'", e);
        }
    }
}
