package io.lumify.csv.model;

import io.lumify.core.model.properties.types.ClientApiLumifyProperty;

public class MappingProperty extends ClientApiLumifyProperty<Mapping> {
    public MappingProperty(String key) {
        super(key, Mapping.class);
    }
}
