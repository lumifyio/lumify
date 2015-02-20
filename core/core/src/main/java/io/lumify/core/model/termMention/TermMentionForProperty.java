package io.lumify.core.model.termMention;

import io.lumify.core.model.properties.types.LumifyProperty;

public class TermMentionForProperty extends LumifyProperty<TermMentionFor, String> {
    public TermMentionForProperty(final String key) {
        super(key);
    }

    @Override
    public String wrap(TermMentionFor value) {
        return value.toString();
    }

    @Override
    public TermMentionFor unwrap(final Object value) {
        if (value == null) {
            return null;
        }
        return TermMentionFor.valueOf(value.toString());
    }
}

