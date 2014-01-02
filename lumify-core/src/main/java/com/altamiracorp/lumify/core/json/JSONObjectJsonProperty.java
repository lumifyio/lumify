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

import org.json.JSONObject;

/**
 * A JSON property whose value is a sub-JSONObject.
 */
public class JSONObjectJsonProperty extends BaseDirectJsonProperty<JSONObject> {
    /**
     * Create a new JSONObjectJsonProperty.
     * @param key the property key
     */
    public JSONObjectJsonProperty(final String key) {
        super(key, JsonType.OBJECT);
    }
}
