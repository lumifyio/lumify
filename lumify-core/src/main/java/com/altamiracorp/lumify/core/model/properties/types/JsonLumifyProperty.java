package com.altamiracorp.lumify.core.model.properties.types;

import org.json.JSONObject;

public class JsonLumifyProperty extends LumifyProperty<JSONObject, String> {
    public JsonLumifyProperty(String key) {
        super(key);
    }

    @Override
    public String wrap(JSONObject value) {
        return value.toString();
    }

    @Override
    public JSONObject unwrap(Object value) {
        if (value == null) {
            return null;
        }
        return new JSONObject(value.toString());
    }
}
