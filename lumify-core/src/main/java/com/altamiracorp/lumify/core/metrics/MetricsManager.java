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

package com.altamiracorp.lumify.core.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

/**
 * Provides access to Lumify Metrics services.
 */
public interface MetricsManager {
    /**
     * Get the name prefix for Counters or Timers for
     * this manager.
     * @param obj the object to generate a prefix for
     * @return the generated name prefix
     */
    String getNamePrefix(final Object obj);
    
    /**
     * Get the name prefix for Counters or Timers for
     * this manager, including the provided qualifier
     * with the generated prefix.
     * @param obj the object to generate a prefix for
     * @param qualifier the qualifier
     * @return the generated name prefix
     */
    String getNamePrefix(final Object obj, final String qualifier);
    
    /**
     * Get the MetricRegistry associated with this MetricsManager.
     * @return the MetricRegistry
     */
    MetricRegistry getRegistry();
    
    /**
     * Get the Counter with the given name, creating it if
     * it does not exist.
     * @param name the name of the Counter
     * @return the requested Counter
     */
    Counter counter(final String name);
    
    /**
     * Get the Timer with the given name, creating it if
     * it does not exist.
     * @param name the name of the Timer
     * @return the requested Timer
     */
    Timer timer(final String name);
}
