package io.lumify.core.model.properties.types;

import org.securegraph.Text;
import org.securegraph.TextIndexHint;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

public class TextLumifyProperty extends LumifyProperty<String, Text> {
    public static TextLumifyProperty all(final String propertyKey) {
        return new TextLumifyProperty(propertyKey, TextIndexHint.ALL);
    }

    private final Set<TextIndexHint> indexHints;

    public TextLumifyProperty(final String key, final TextIndexHint... hints) {
        this(key, hints != null && hints.length > 0 ? Arrays.asList(hints) : TextIndexHint.ALL);
    }

    public TextLumifyProperty(final String key, final Collection<TextIndexHint> hints) {
        super(key);
        checkNotNull(hints, "index hints must be provided");
        this.indexHints = EnumSet.copyOf(hints);
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
