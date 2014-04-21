package com.altamiracorp.lumify.storm.util;

import backtype.storm.tuple.Fields;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsEqual;

/**
 * Matcher for validating Storm Fields objects in unit tests.
 */
public class FieldsMatcher extends TypeSafeMatcher<Fields> {
    private Matcher<List<String>> fieldsMatcher;
    
    public FieldsMatcher(final String... fields) {
        this(fields.length > 0 ? Arrays.asList(fields) : Collections.EMPTY_LIST);
    }
    
    public FieldsMatcher(final List<String> fields) {
        fieldsMatcher = new IsEqual<List<String>>(Collections.unmodifiableList(new ArrayList<String>(fields)));
    }
    
    @Override
    protected boolean matchesSafely(final Fields inputFields) {
        return fieldsMatcher.matches(inputFields.toList());
    }

    @Override
    public void describeTo(final Description description) {
        fieldsMatcher.describeTo(description);
    }
    
    @Factory
    public static <T> Matcher<Fields> sameFields(final String... fields) {
        return new FieldsMatcher(fields);
    }
}
