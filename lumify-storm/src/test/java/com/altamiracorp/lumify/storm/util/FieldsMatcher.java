/*
 * Copyright 2014 Altamira Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
