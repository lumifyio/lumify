package io.lumify.core.model.properties.types;

import io.lumify.core.util.ClientApiConverter;
import io.lumify.web.clientapi.model.VisibilityJson;

public class VisibilityJsonLumifyProperty extends LumifyProperty<VisibilityJson, String> {
    public VisibilityJsonLumifyProperty(String key) {
        super(key);
    }

    @Override
    public String wrap(VisibilityJson value) {
        return ClientApiConverter.clientApiObjectToJsonString(value);
    }

    @Override
    public VisibilityJson unwrap(Object value) {
        if (value == null) {
            return null;
        }
        String valueStr;
        if (value instanceof String) {
            valueStr = (String) value;
        } else {
            valueStr = value.toString();
        }
        return ClientApiConverter.toClientApi(valueStr, VisibilityJson.class);
    }
}
