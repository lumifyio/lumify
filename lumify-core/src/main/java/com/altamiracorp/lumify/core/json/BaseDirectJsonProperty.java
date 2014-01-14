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

package com.altamiracorp.lumify.core.json;

/**
 * Base class for JsonProperty subtypes whose output and JSON values
 * are the same type.
 */
public abstract class BaseDirectJsonProperty<T> extends JsonProperty<T, T> {
    /**
     * Create a new BaseDirectJsonProperty.
     * @param key the property key
     * @param type the JsonType
     */
    protected BaseDirectJsonProperty(final String key, final JsonType type) {
        super(key, type);
    }

    @Override
    protected final T fromJSON(final T jsonValue) {
        return jsonValue;
    }

    @Override
    protected final T toJSON(final T value) {
        return value;
    }
}
