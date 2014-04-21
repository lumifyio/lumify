package com.altamiracorp.lumify.core.model.properties.types;

import com.altamiracorp.securegraph.property.StreamingPropertyValue;

/**
 * An IdentityLumifyProperty for StreamingPropertyValues.
 */
public class StreamingLumifyProperty extends IdentityLumifyProperty<StreamingPropertyValue> {
    /**
     * Create a new StreamingLumifyProperty.
     * @param key the property key
     */
    public StreamingLumifyProperty(String key) {
        super(key);
    }
}
