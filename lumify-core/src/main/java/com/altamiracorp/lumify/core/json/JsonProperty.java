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

import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A JsonProperty is a utility class for setting and retrieving property values
 * of a particular type on a JSONObject.
 * @param <P> the desired type of the property value
 * @param <J> the type of the JSON property value
 */
public abstract class JsonProperty<P, J> {
    /**
     * The class logger.
     */
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(JsonProperty.class);
    
    /**
     * Enumeration of JSON property types.  This is used internally to
     * retrieve the value from the JSON object before converting it to
     * the target value.
     */
    protected static enum JsonType { ARRAY, BOOLEAN, DOUBLE, INTEGER, LONG, OBJECT, STRING }
    
    /**
     * The property key.
     */
    private final String propertyKey;
    
    /**
     * The JSON type.
     */
    private final JsonType jsonType;
    
    /**
     * Create a new JsonProperty.
     * @param key the property key
     * @param type the JSON type of the property
     */
    protected JsonProperty(final String key, final JsonType type) {
        propertyKey = key;
        jsonType = type;
    }
    
    /**
     * Convert the JSON property value to the output property value.
     * @param jsonValue the JSON value
     * @return the converted output value or <code>null</code> if an error occurs during conversion
     */
    protected abstract P fromJSON(final J jsonValue);
    
    /**
     * Convert the input property value to its JSON representation
     * @param value the input value
     * @return the converted JSON value or <code>null</code> if an error occurs during conversion
     */
    protected abstract J toJSON(final P value);
    
    public final String getPropertyKey() {
        return propertyKey;
    }
    
    /**
     * Get the value of this property from the provided JSON object or
     * return <code>null</code> if it is not set (JSON-null) or an error
     * occurs during conversion to the configured type.
     * @param json the JSON object to retrieve the value from
     * @return the property value or <code>null</code> if it was not set
     */
    public final P getFrom(final JSONObject json) {
        return getFrom(json, null);
    }
    
    /**
     * Get the value of this property from the provided JSON object or
     * return the provided default value if it is not set (JSON-null) or
     * an error occurs during conversion to the configured type.
     * @param json the JSON object to retrieve the value from
     * @param defaultValue the default value to return
     * @return the property value or the default value if it was not set
     */
    public final P getFrom(final JSONObject json, final P defaultValue) {
        P value = defaultValue;
        if (json != null && !json.isNull(propertyKey)) {
            try {
                J jsonValue = null;
                switch (jsonType) {
                    case ARRAY:
                        jsonValue = (J) json.getJSONArray(propertyKey);
                        break;
                    case BOOLEAN:
                        jsonValue = (J) Boolean.valueOf(json.getBoolean(propertyKey));
                        break;
                    case DOUBLE:
                        jsonValue = (J) Double.valueOf(json.getDouble(propertyKey));
                        break;
                    case INTEGER:
                        jsonValue = (J) Integer.valueOf(json.getInt(propertyKey));
                        break;
                    case LONG:
                        jsonValue = (J) Long.valueOf(json.getLong(propertyKey));
                        break;
                    case OBJECT:
                        jsonValue = (J) json.getJSONObject(propertyKey);
                        break;
                    case STRING:
                        jsonValue = (J) json.getString(propertyKey);
                        break;
                    default:
                        throw new IllegalStateException(String.format("Unknown JSON Type: %s", jsonType));
                }
                value = fromJSON(jsonValue);
                if (value == null) {
                    value = defaultValue;
                }
            } catch (JSONException je) {
                LOGGER.trace("Error retrieving property [%s] from JSON: %s", propertyKey, json, je);
            }
        }
        return value;
    }
    
    /**
     * Set this property of the provided JSONObject.
     * @param json the target JSONObject
     * @param value the value to set
     */
    public final void setOn(final JSONObject json, final P value) {
        J jsonValue = toJSON(value);
        if (jsonValue == null) {
            json.put(propertyKey, JSONObject.NULL);
        } else {
            json.put(propertyKey, jsonValue);
        }
    }
}
