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

package com.altamiracorp.lumify.core.model.properties;

import com.altamiracorp.securegraph.Vertex;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import java.util.Collections;

/**
 * A LumifyProperty provides convenience methods for converting standard
 * property values to and from their raw types to the types required to
 * store them in a SecureGraph instance.
 * @param <TRaw> the raw value type for this property
 * @param <TGraph> the value type presented to SecureGraph for this property
 */
public abstract class LumifyProperty<TRaw, TGraph> {
    /**
     * The property key.
     */
    private final String key;

    /**
     * The raw conversion function.
     */
    private final Function<Object, TRaw> rawConverter;

    /**
     * Create a new LumifyProperty with the given key.
     * @param inKey the property key
     */
    protected LumifyProperty(final String inKey) {
        this.key = inKey;
        this.rawConverter = new RawConverter();
    }

    /**
     * Convert the raw value to an appropriate value for storage
     * in SecureGraph.
     * @param value the raw value
     * @return the SecureGraph value type representing the input value
     */
    public abstract TGraph wrap(final TRaw value);

    /**
     * Convert the SecureGraph value to its original raw type.
     * @param value the SecureGraph value
     * @return the raw value represented by the input SecureGraph value
     */
    public abstract TRaw unwrap(final TGraph value);

    /**
     * Get the value of this property from the provided Vertex.
     * @param vertex the vertex
     * @return the value of this property on the given Vertex or null if it is not configured
     */
    public final TRaw getValue(final Vertex vertex) {
        Object value = vertex.getPropertyValue(key);
        return value != null ? rawConverter.apply(value) : null;
    }

    /**
     * Get all values of this property from the provided Vertex.
     * @param vertex the vertex
     * @return an Iterable over the values of this property on the given Vertex
     */
    @SuppressWarnings("unchecked")
    public final Iterable<TRaw> getValues(final Vertex vertex) {
        Iterable<Object> values = vertex.getPropertyValues(key);
        return values != null ? Iterables.transform(values, rawConverter) : Collections.EMPTY_LIST;
    }

    /**
     * Function that converts the values returned by the Vertex.getPropertyValue()
     * methods to the configured TRaw type.
     */
    private class RawConverter implements Function<Object, TRaw> {
        @Override
        @SuppressWarnings("unchecked")
        public TRaw apply(final Object input) {
            return unwrap((TGraph) input);
        }
    }
}
