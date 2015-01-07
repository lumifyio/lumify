package io.lumify.core.model.properties.types;

import io.lumify.core.util.ClientApiConverter;
import io.lumify.web.clientapi.model.VisibilityJson;

public class VisibilityJsonLumifyProperty extends ClientApiLumifyProperty<VisibilityJson> {
    public VisibilityJsonLumifyProperty(String key) {
        super(key, VisibilityJson.class);
    }
}
