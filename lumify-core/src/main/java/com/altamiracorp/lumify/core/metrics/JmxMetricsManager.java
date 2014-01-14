package com.altamiracorp.lumify.core.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import java.util.concurrent.atomic.AtomicInteger;

public class JmxMetricsManager implements MetricsManager {
    private static final MetricRegistry REGISTRY;
    private static final JmxReporter JMX_REPORTER;
    private static final AtomicInteger ID = new AtomicInteger(0);

    static {
        REGISTRY = new MetricRegistry();
        JMX_REPORTER = JmxReporter.forRegistry(REGISTRY).build();
        JMX_REPORTER.start();
    }

    /**
     * Get the next ID.
     * @return the next ID
     */
    private static int nextId() {
        return ID.getAndIncrement();
    }

    @Override
    public MetricRegistry getRegistry() {
        return REGISTRY;
    }

    @Override
    public String getNamePrefix(final Object obj) {
        return String.format("%s.%d.", obj.getClass().getName(), JmxMetricsManager.nextId());
    }

    @Override
    public String getNamePrefix(final Object obj, final String qualifier) {
        return String.format("%s.%s-%d.", obj.getClass().getName(), qualifier, JmxMetricsManager.nextId());
    }

    @Override
    public Counter counter(final String name) {
        return getRegistry().counter(name);
    }

    @Override
    public Timer timer(final String name) {
        return getRegistry().timer(name);
    }
}
