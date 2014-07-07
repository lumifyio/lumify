package io.lumify.mapping.predicate;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * An implementation of MappingPredicate that matches input values
 * if all sub-predicates match the input value.
 * @param <T> the type of values being matched
 */
@JsonTypeName("and")
public final class AndPredicate<T> implements MappingPredicate<T> {
    /**
     * The predicates that will be and-ed.
     */
    private final List<MappingPredicate<T>> predicates;

    /**
     * Create a new AndPredicate.
     * @param preds the predicates to AND together
     */
    public AndPredicate(final MappingPredicate<T>... preds) {
        this(Arrays.asList(preds));
    }

    /**
     * Create a new AndPredicate.
     * @param preds the predicates to AND together
     */
    @JsonCreator
    public AndPredicate(@JsonProperty("predicates") final List<MappingPredicate<T>> preds) {
        checkNotNull(preds, "predicates must be provided");
        List<MappingPredicate<T>> myPreds = new ArrayList<MappingPredicate<T>>(preds);
        // remove all null values from myPreds
        while (myPreds.remove(null)) {}
        checkArgument(!myPreds.isEmpty(), "at least one predicate must be provided");
        this.predicates = Collections.unmodifiableList(myPreds);
    }

    @Override
    public boolean matches(final T value) {
        boolean match = true;
        for (MappingPredicate<T> pred : predicates) {
            if (!pred.matches(value)) {
                match = false;
                break;
            }
        }
        return match;
    }

    @JsonProperty("predicates")
    public List<MappingPredicate<T>> getPredicates() {
        return predicates;
    }

    @Override
    public String toString() {
        return String.format("AND( %s )", predicates);
    }
}
