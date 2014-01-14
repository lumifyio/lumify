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

package com.altamiracorp.lumify.storm;

import backtype.storm.tuple.Tuple;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Base class for Bolts that perform operations on a JSONObject
 * found in the input Tuple in a known field name.  Subclasses
 * may specify whether to ignore or fail Tuples without valid
 * JSONObjects.  The default behavior is to ignore Tuples with
 * missing or empty JSON and fail Tuples with invalid JSON.
 * <p>
 * Failures are triggered by throwing an IllegalArgumentException
 * from the safeExecute() method if either policy is set to FAIL.
 * Failures resulting from invalid JSON will contain the triggering
 * JSONException as the cause of the IllegalArgumentException.
 * </p>
 */
public abstract class BaseLumifyJsonBolt extends BaseLumifyMetricsBolt {
    /**
     * The name of the JSON field in the Tuple.
     */
    public static final String JSON_FIELD = "json";
    
    /**
     * Indicates whether Tuples with missing or invalid JSON should
     * be ignored and ACK'ed without further processing or failed
     * and marked as an error.
     */
    protected static enum JsonHandlingPolicy { IGNORE, FAIL }
    
    /**
     * The missing JSON policy.  This policy is applied if
     * the JSON Tuple field is null, empty or whitespace.
     */
    private final JsonHandlingPolicy missingJsonPolicy;
    
    /**
     * The invalid JSON policy.  This policy is applied if
     * the JSON Tuple field contains an invalid JSON string.
     */
    private final JsonHandlingPolicy invalidJsonPolicy;
    
    /**
     * Create a new BaseLumifyJsonBolt with the default policies:
     * IGNORE missing JSON and FAIL invalid JSON.
     */
    protected BaseLumifyJsonBolt() {
        this(JsonHandlingPolicy.IGNORE, JsonHandlingPolicy.FAIL);
    }
    
    /**
     * Create a new BaseLumifyJsonBolt with the specified policies for
     * handling missing or invalid JSON.
     * @param missingPolicy the policy for handling missing JSON fields
     * @param invalidPolicy the policy for handling invalid JSON fields
     */
    protected BaseLumifyJsonBolt(final JsonHandlingPolicy missingPolicy, final JsonHandlingPolicy invalidPolicy) {
        assert missingPolicy != null;
        assert invalidPolicy != null;
        missingJsonPolicy = missingPolicy;
        invalidJsonPolicy = invalidPolicy;
    }

    @Override
    protected final void safeExecute(final Tuple input) throws Exception {
        String jsonStr = input.getStringByField(JSON_FIELD);
        if (jsonStr != null && !jsonStr.trim().isEmpty()) {
            JSONObject json;
            try {
                json = new JSONObject(jsonStr);
            } catch (JSONException je) {
                json = null;
                if (JsonHandlingPolicy.FAIL == invalidJsonPolicy) {
                    throw new IllegalArgumentException(String.format("Unable to parse JSON from input Field %s. Value: %s"), je);
                }
            }
            // only continue processing if we successfully deserialized the JSON string;
            // if the invalidJsonPolicy is IGNORE, json will be null
            if (json != null) {
                processJson(json, input);
            }
        } else if (JsonHandlingPolicy.FAIL == missingJsonPolicy) {
            throw new IllegalArgumentException(String.format("No JSON found in input Field %s", JSON_FIELD));
        }
    }
    
    protected abstract void processJson(final JSONObject json, final Tuple input) throws Exception;
}
