package io.lumify.core.model.properties.types;

public class StringLumifyProperty extends LumifyProperty<String, String> {
    public StringLumifyProperty(final String key) {
        super(key);
    }

    @Override
    public String wrap(final String value) {
        return value;
    }

    @Override
    public String unwrap(final Object value) {
        return value.toString();
    }
}
