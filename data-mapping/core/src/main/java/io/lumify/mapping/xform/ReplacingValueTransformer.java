package io.lumify.mapping.xform;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.lumify.mapping.predicate.MappingPredicate;

/**
 * A ValueTransformer that replaces a set of target values with
 * a configured value before returning to the caller.  Values that
 * do not match the target criteria are returned unchanged.
 * @param <T> the type of value returned by this filter
 */
@JsonTypeName("replace")
public class ReplacingValueTransformer<T> implements ValueTransformer<T> {
    /**
     * The delegate ValueTransformer.
     */
    private final ValueTransformer<T> xform;

    /**
     * The predicate indicating the values to replace.
     */
    private final MappingPredicate<T> targetCriteria;

    /**
     * The replacement value for values matching the target criteria.
     */
    private final T replacementValue;

    /**
     * Create a new FilteringValueTransformer.
     * @param delegate the ValueTransformer that will return the values to replace
     * @param criteria the target criteria
     * @param value the value to return when values match the target criteria
     */
    @JsonCreator
    public ReplacingValueTransformer(@JsonProperty("xform") final ValueTransformer<T> delegate,
            @JsonProperty("targetCriteria") final MappingPredicate<T> criteria,
            @JsonProperty(value="replacementValue",required=false) final T value) {
        checkNotNull(delegate, "delegate ValueTransformer (xform) must be provided");
        checkNotNull(criteria, "targetCriteria must be provided");
        this.xform = delegate;
        this.targetCriteria = criteria;
        this.replacementValue = value;
    }

    @Override
    public T transform(final String input) {
        T value = xform.transform(input);
        return targetCriteria.matches(value) ? replacementValue : value;
    }

    @JsonProperty("xform")
    public ValueTransformer<T> getXform() {
        return xform;
    }

    @JsonProperty("targetCriteria")
    public MappingPredicate<T> getTargetCriteria() {
        return targetCriteria;
    }

    @JsonProperty("replacementValue")
    public T getReplacementValue() {
        return replacementValue;
    }
}
