package com.altamiracorp.lumify.core.model.properties.types;

import static com.google.common.base.Preconditions.*;

import com.altamiracorp.securegraph.Text;
import com.altamiracorp.securegraph.TextIndexHint;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * An implementation of LumifyProperty that converts String-valued
 * properties into SecureGraph Text properties with a configured
 * set of indexing hints.
 */
public class TextLumifyProperty extends LumifyProperty<String, Text> {
    /**
     * Create a TextLumifyProperty that indexes in all possible ways.
     * @param key the property key
     * @return a TextLumifyProperty for the given key that uses all indexing hints
     */
    public static TextLumifyProperty all(final String propertyKey) {
        return new TextLumifyProperty(propertyKey, TextIndexHint.ALL);
    }

    /**
     * The indexing hints for this property.
     */
    private final Set<TextIndexHint> indexHints;

    /**
     * Create a new TextLumifyProperty.  If hints are not explicitly provided,
 TextIndexHint.ALL will be used as the indexing hints.
     * @param key the property key
     * @param hints the index hints
     */
    public TextLumifyProperty(final String key, final TextIndexHint... hints) {
        this(key, hints != null && hints.length > 0 ? Arrays.asList(hints) : TextIndexHint.ALL);
    }

    /**
     * Create a new TextLumifyProperty.
     * @param key the property key
     * @param hints the index hints
     */
    public TextLumifyProperty(final String key, final Collection<TextIndexHint> hints) {
        super(key);
        checkNotNull(hints, "index hints must be provided");
        this.indexHints = EnumSet.copyOf(hints);
    }

    public Set<TextIndexHint> getIndexHints() {
        return Collections.unmodifiableSet(indexHints);
    }

    @Override
    public Text wrap(final String value) {
        return new Text(value, indexHints);
    }

    @Override
    public String unwrap(final Object value) {
        String output = null;
        if (value instanceof Text) {
            output = ((Text) value).getText();
        } else if (value != null) {
            output = value.toString();
        }
        return output;
    }
}
