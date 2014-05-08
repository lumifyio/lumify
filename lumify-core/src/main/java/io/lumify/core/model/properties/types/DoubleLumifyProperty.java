package io.lumify.core.model.properties.types;

import org.securegraph.Element;

public class DoubleLumifyProperty extends IdentityLumifyProperty<Double> {
    public DoubleLumifyProperty(String key) {
        super(key);
    }

    public double getPropertyValue(Element element, double defaultValue) {
        Double nullable = getPropertyValue(element);
        if (nullable == null) {
            return defaultValue;
        }
        return nullable;
    }
}
