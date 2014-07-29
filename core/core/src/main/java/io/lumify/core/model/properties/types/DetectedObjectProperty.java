package io.lumify.core.model.properties.types;

import io.lumify.core.ingest.ArtifactDetectedObject;
import org.json.JSONObject;

public class DetectedObjectProperty extends LumifyProperty<ArtifactDetectedObject, String> {
    public DetectedObjectProperty(String key) {
        super(key);
    }

    @Override
    public String wrap(ArtifactDetectedObject value) {
        return value.toJson().toString();
    }

    @Override
    public ArtifactDetectedObject unwrap(Object value) {
        if (value == null) {
            return null;
        }
        return new ArtifactDetectedObject(new JSONObject(value.toString()));
    }
}
