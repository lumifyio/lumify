package io.lumify.core.model.properties.types;

import org.securegraph.Element;
import org.securegraph.type.GeoPoint;

public class GeoPointLumifyProperty extends IdentityLumifyProperty<GeoPoint> {
    public GeoPointLumifyProperty(String key) {
        super(key);
    }

    public GeoPoint getPropertyValue(Element element, GeoPoint defaultValue) {
        GeoPoint nullable = getPropertyValue(element);
        if (nullable == null) {
            return defaultValue;
        }
        return nullable;
    }
}
