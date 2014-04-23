package io.lumify.core.model.properties.types;

import org.securegraph.property.StreamingPropertyValue;

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
