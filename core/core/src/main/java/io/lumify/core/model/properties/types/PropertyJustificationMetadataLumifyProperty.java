package io.lumify.core.model.properties.types;

import io.lumify.core.model.PropertyJustificationMetadata;
import org.json.JSONObject;

public class PropertyJustificationMetadataLumifyProperty extends LumifyProperty<PropertyJustificationMetadata, String> {
    public PropertyJustificationMetadataLumifyProperty(final String key) {
        super(key);
    }

    @Override
    public String wrap(PropertyJustificationMetadata value) {
        return value.toJson().toString();
    }

    @Override
    public PropertyJustificationMetadata unwrap(Object value) {
        if (value == null) {
            return null;
        }
        return new PropertyJustificationMetadata(new JSONObject(value.toString()));
    }
}
