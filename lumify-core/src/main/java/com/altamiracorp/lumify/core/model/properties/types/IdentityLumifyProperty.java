package com.altamiracorp.lumify.core.model.properties.types;

/**
 * A LumifyProperty whose raw and SecureGraph types are the same.
 */
public class IdentityLumifyProperty<T> extends LumifyProperty<T, T> {
    /**
     * Create a new IdentityLumifyProperty.
     * @param key the property key
     */
    public IdentityLumifyProperty(final String key) {
        super(key);
    }

    @Override
    public T wrap(final T value) {
        return value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T unwrap(final Object value) {
        return (T) value;
    }
}
