package com.altamiracorp.lumify.core.bootstrap;

import com.altamiracorp.lumify.core.config.Configuration;
import com.google.inject.Binder;

/**
 * A BootstrapBindingProvider can add Guice bindings to the Lumify Bootstrap Module.
 * Implementations are automatically discovered by the Lumify Bootstrapper and will be
 * instantiated using an empty constructor.
 */
public interface BootstrapBindingProvider {
    /**
     * Add the bindings defined by this BootstrapBindingProvider to
     * the Lumify Bootstrap module.
     * @param binder the Binder that configures the Bootstrapper
     * @param configuration the Lumify Configuration
     */
    void addBindings(final Binder binder, final Configuration configuration);
}
