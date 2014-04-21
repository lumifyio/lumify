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
