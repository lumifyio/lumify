package com.altamiracorp.lumify.core.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

public class MetricsManager {
    private static final MetricRegistry registry;
    private static final JmxReporter jmxReporter;
    private static int id = 0;

    static {
        registry = new MetricRegistry();
        jmxReporter = JmxReporter.forRegistry(registry).build();
        jmxReporter.start();
    }

    public MetricRegistry getRegistry() {
        return registry;
    }

    public String getNamePrefix(Object o) {
        return o.getClass().getName() + "." + getNextId() + ".";
    }

    public String getNamePrefix(Object o, String name) {
        return o.getClass().getName() + "." + name + "-" + getNextId() + ".";
    }

    private int getNextId() {
        return id++;
    }

    public Counter counter(String name) {
        return getRegistry().counter(name);
    }

    public Timer timer(String name) {
        return getRegistry().timer(name);
    }
}
