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
 * if any sub-predicate matches the input value.
 * @param <T> the type of values being matched
 */
@JsonTypeName("or")
public final class OrPredicate<T> implements MappingPredicate<T> {
    /**
     * The predicates that will be or-ed.
     */
    private final List<MappingPredicate<T>> predicates;

    /**
     * Create a new OrPredicate.
     * @param preds the predicates to OR together
     */
    public OrPredicate(final MappingPredicate<T>... preds) {
        this(Arrays.asList(preds));
    }

    /**
     * Create a new OrPredicate.
     * @param preds the predicates to OR together
     */
    @JsonCreator
    public OrPredicate(@JsonProperty("predicates") final List<MappingPredicate<T>> preds) {
        checkNotNull(preds, "predicates must be provided");
        List<MappingPredicate<T>> myPreds = new ArrayList<>(preds);
        // remove all null values from myPreds
        while (myPreds.remove(null)) {}
        checkArgument(!myPreds.isEmpty(), "at least one predicate must be provided");
        this.predicates = Collections.unmodifiableList(myPreds);
    }

    @Override
    public boolean matches(final T value) {
        boolean match = false;
        for (MappingPredicate<T> pred : predicates) {
            if (pred.matches(value)) {
                match = true;
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
        return String.format("OR( %s )", predicates);
    }
}
