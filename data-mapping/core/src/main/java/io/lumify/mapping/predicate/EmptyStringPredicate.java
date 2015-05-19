package io.lumify.mapping.predicate;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * This predicate matches Strings whose values are isEmpty or whitespace-only.
 */
@JsonTypeName("emptyStr")
public class EmptyStringPredicate implements MappingPredicate<String> {
    @Override
    public boolean matches(final String value) {
        return value != null && value.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "? eq \"\"";
    }
}
