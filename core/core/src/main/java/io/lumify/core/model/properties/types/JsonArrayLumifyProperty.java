package io.lumify.core.model.properties.types;

import io.lumify.core.util.JSONUtil;
import org.json.JSONArray;

public class JsonArrayLumifyProperty extends LumifyProperty<JSONArray, String> {
    public JsonArrayLumifyProperty(String key) {
        super(key);
    }

    @Override
    public String wrap(JSONArray value) {
        return value.toString();
    }

    @Override
    public JSONArray unwrap(Object value) {
        if (value == null) {
            return null;
        }
        return JSONUtil.parseArray(value.toString());
    }
}
