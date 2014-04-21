package com.altamiracorp.lumify.core.bootstrap;

import com.altamiracorp.lumify.core.config.Configuration;

/**
 * Utility methods for bootstrapping Lumify.
 */
public final class BootstrapUtils {
    /**
     * Gets the Class indicated in the provided Configuration property.  If the Class is required,
     * this method will throw a BootstrapException if it was not configured or was configured to
     * an unknown Class name; otherwise it will return <code>null</code>.
     *
     * @param <T>      the type of Class being retrieved
     * @param config   the Configuration to search
     * @param key      the Configuration key containing the class name
     * @param required <code>true</code> if this Class must be configured
     * @return the configured Class or <code>null</code> if not required and the Class could not be identified
     * @throws BootstrapException if the Class is required and could not be identified
     */
    public static <T> Class<? extends T> getConfiguredClass(final Configuration config, final String key, final boolean required) {
        Class<? extends T> configuredClass;
        try {
            configuredClass = config.getClass(key, null);
        } catch (ClassNotFoundException cnfe) {
            configuredClass = null;
        }
        if (configuredClass == null && required) {
            throw new BootstrapException("%s must be configured and set to a valid class name.", key);
        }
        return configuredClass;
    }

    /**
     * Utility class constructor.
     */
    private BootstrapUtils() {
        throw new UnsupportedOperationException("Utility class should not be instantiated.");
    }
}
