package io.lumify.core.model.properties.types;

import org.securegraph.Element;

public class LongLumifyProperty extends IdentityLumifyProperty<Long> {
    public LongLumifyProperty(String key) {
        super(key);
    }

    public long getPropertyValue(Element element, long defaultValue) {
        Long nullable = getPropertyValue(element);
        if (nullable == null) {
            return defaultValue;
        }
        return nullable;
    }
}
