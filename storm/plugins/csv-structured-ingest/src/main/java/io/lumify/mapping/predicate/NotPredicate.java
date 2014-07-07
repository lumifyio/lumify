package io.lumify.mapping.predicate;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * This predicate negates the match from another predicate.
 * @param <T> the type of value being matched
 */
@JsonTypeName("not")
public final class NotPredicate<T> implements MappingPredicate<T> {
    /**
     * The predicate to negate.
     */
    private final MappingPredicate<T> predicate;

    /**
     * Create a new NotPredicate.
     * @param pred the predicate to negate
     */
    @JsonCreator
    public NotPredicate(@JsonProperty("predicate") final MappingPredicate<T> pred) {
        checkNotNull(pred, "predicate must be provided");
        this.predicate = pred;
    }

    @Override
    public boolean matches(final T value) {
        return !predicate.matches(value);
    }

    @JsonProperty("predicate")
    public MappingPredicate<T> getPredicate() {
        return predicate;
    }

    @Override
    public String toString() {
        return String.format("NOT( %s )", predicate);
    }
}
