package io.lumify.mapping.predicate;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * A predicate that matches if the input value is equal to a configured value.
 * This predicate uses the Object.equals() method to determine equality.
 * @param <T> the type of value being matched
 */
@JsonTypeName("eq")
public final class EqualsPredicate<T> implements MappingPredicate<T> {
    /**
     * The value to match.
     */
    private final T value;

    /**
     * Create a new EqualsPredicate.
     * @param val the value to match
     */
    @JsonCreator
    public EqualsPredicate(@JsonProperty("value") final T val) {
        checkNotNull(val, "value cannot be null");
        this.value = val;
    }

    @Override
    public boolean matches(final T input) {
        return value.equals(input);
    }

    @JsonProperty("value")
    public T getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.format("? eq %s", value);
    }
}
