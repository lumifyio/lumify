package io.lumify.mapping.predicate;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * This predicate matches if the input value is <code>null</code>
 */
@JsonTypeName("isNull")
public final class NullPredicate<T> implements MappingPredicate<T> {
    @Override
    public boolean matches(final T value) {
        return value == null;
    }

    @Override
    public String toString() {
        return "? is null";
    }
}
