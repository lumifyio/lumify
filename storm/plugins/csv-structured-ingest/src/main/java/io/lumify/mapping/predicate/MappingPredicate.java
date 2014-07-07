package io.lumify.mapping.predicate;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

/**
 * A Jackson serializable Predicate used by Lumify mappings to match
 * values.
 * @param <T> the type of values this predicate matches
 */
@JsonTypeInfo(include=As.PROPERTY, property="op", use=Id.NAME)
@JsonSubTypes({
    @Type(AndPredicate.class),
    @Type(EmptyStringPredicate.class),
    @Type(EqualsPredicate.class),
    @Type(NotPredicate.class),
    @Type(NullPredicate.class),
    @Type(OrPredicate.class),
    @Type(StringEqualsPredicate.class)
})
public interface MappingPredicate<T> {
    /**
     * Does the input value match this predicate?
     * @param value the value to match
     * @return <code>true</code> if the value meets the criteria of this predicate
     */
    boolean matches(final T value);
}
