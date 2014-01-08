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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * A matcher that verifies that all bytes found in a provided InputStream
 * match a provided set of bytes.  This Matcher is only supported for InputStreams
 * where markSupported() returns true.  If the target InputStream does not support
 * mark/reset, an IllegalArgumentException will be thrown.
 */
public class InputStreamContentMatcher extends TypeSafeMatcher<InputStream> {
    private final byte[] matchBytes;
    
    public InputStreamContentMatcher(final byte[] match) {
        matchBytes = match;
    }

    @Override
    protected boolean matchesSafely(final InputStream stream) {
        if (!stream.markSupported()) {
            throw new IllegalArgumentException("InputStreamContentMatcher can only match InputStreams that support mark/reset.");
        }
        try {
            ByteArrayOutputStream inputBytes = new ByteArrayOutputStream();
            stream.mark(Integer.MAX_VALUE);
            IOUtils.copyLarge(stream, inputBytes);
            stream.reset();
            return Arrays.equals(matchBytes, inputBytes.toByteArray());
        } catch (IOException unused) {
            return false;
        }
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(String.format("stream of %s", Arrays.toString(matchBytes)));
    }
    
    @Factory
    public static <T> Matcher<InputStream> containsBytes(final byte[] bytes) {
        return new InputStreamContentMatcher(bytes);
    }
}
