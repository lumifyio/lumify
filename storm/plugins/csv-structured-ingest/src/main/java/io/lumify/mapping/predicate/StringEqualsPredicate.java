package io.lumify.mapping.predicate;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * This predicate matches values whose trimmed toString() method matches a provided
 * String. Case sensitivity is configurable.
 * @param <T> the type of value being matched
 */
@JsonTypeName("stringEq")
public final class StringEqualsPredicate<T> implements MappingPredicate<T> {
    /**
     * The default case sensitivity setting: Case-insensitive.
     */
    private static final boolean DEFAULT_CASE_SENSITIVE = false;

    /**
     * Should this match be case sensitive?
     */
    private final boolean caseSensitive;

    /**
     * The string to match.
     */
    private final String value;

    /**
     * Create a new StringEqualsPredicate.
     * @param val the string to match
     * @param matchCase should this be case sensitive? (default: false)
     */
    @JsonCreator
    public StringEqualsPredicate(@JsonProperty("value") final String val,
            @JsonProperty(value="caseSensitive", required=false) final Boolean matchCase) {
        checkNotNull(val, "value must be provided");
        this.value = val.trim();
        this.caseSensitive = matchCase != null ? matchCase : DEFAULT_CASE_SENSITIVE;
    }

    @Override
    public boolean matches(final T input) {
        String strValue = input != null ? input.toString() : null;
        boolean match = false;
        if (strValue != null) {
            match = caseSensitive ? value.equals(strValue.trim()) : value.equalsIgnoreCase(strValue.trim());
        }
        return match;
    }

    @JsonProperty("caseSensitive")
    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    @JsonProperty("value")
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.format("(case: %s) ? eq %s", caseSensitive, value);
    }
}
