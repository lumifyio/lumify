package io.lumify.core.model.properties;

import io.lumify.core.model.properties.types.StringLumifyProperty;
import org.securegraph.TextIndexHint;

/**
 * LumifyProperties that apply to both raw and resolved entities stored in the Lumify system.
 */
public class EntityLumifyProperties {
    public static final StringLumifyProperty SOURCE = new StringLumifyProperty("http://lumify.io#source");
    public static final StringLumifyProperty IMAGE_VERTEX_ID = new StringLumifyProperty("http://lumify.io#entityImageVertexId");

    private EntityLumifyProperties() {
        throw new UnsupportedOperationException("do not construct utility class");
    }
}
