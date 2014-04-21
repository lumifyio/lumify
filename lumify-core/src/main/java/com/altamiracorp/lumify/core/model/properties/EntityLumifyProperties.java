package com.altamiracorp.lumify.core.model.properties;

import com.altamiracorp.lumify.core.model.properties.types.IdentityLumifyProperty;
import com.altamiracorp.lumify.core.model.properties.types.TextLumifyProperty;
import com.altamiracorp.securegraph.type.GeoPoint;

/**
 * LumifyProperties that apply to both raw and resolved entities stored in the Lumify system.
 */
public class EntityLumifyProperties {
    public static final IdentityLumifyProperty<GeoPoint> GEO_LOCATION = new IdentityLumifyProperty<GeoPoint>("http://lumify.io/dev#geolocation");
    public static final TextLumifyProperty SOURCE = TextLumifyProperty.all("http://lumify.io#source");

    private EntityLumifyProperties() {
        throw new UnsupportedOperationException("do not construct utility class");
    }
}
