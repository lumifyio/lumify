package com.altamiracorp.lumify.core.model.properties.types;

import com.altamiracorp.securegraph.Element;

public class IntegerLumifyProperty extends IdentityLumifyProperty<Integer> {
    public IntegerLumifyProperty(String key) {
        super(key);
    }

    public int getPropertyValue(Element element, int defaultValue) {
        Integer nullable = getPropertyValue(element);
        if (nullable == null) {
            return defaultValue;
        }
        return nullable;
    }
}
