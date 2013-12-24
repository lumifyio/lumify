package com.altamiracorp.lumify.web.guice.modules;

import com.altamiracorp.lumify.core.BootstrapBase;
import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.web.AuthenticationProvider;
import com.google.inject.Module;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Wires up the required injections for the web application
 */
public class Bootstrap extends BootstrapBase {
    private final Configuration configuration;

    protected Bootstrap(final Configuration config) {
        super(config);
        configuration = config;
    }

    public static Module create(Configuration config) {
        checkNotNull(config);
        return new Bootstrap(config);
    }

    @Override
    protected void configure() {
        super.configure();
        //bind(MapConfig.class).toInstance(configuration);
        bind(Configuration.class).toInstance(configuration);
        bind(AuthenticationProvider.class).to(getAuthenticationProviderClass());
    }

    private Class<AuthenticationProvider> getAuthenticationProviderClass() {
        String authProviderClass = configuration.get(Configuration.AUTHENTICATION_PROVIDER);
        if (authProviderClass == null) {
            throw new RuntimeException("No " + Configuration.AUTHENTICATION_PROVIDER + " config property set.");
        }

        try {
            return (Class<AuthenticationProvider>) Class.forName(authProviderClass);
        } catch (Exception e) {
            throw new RuntimeException("Unable to create AuthenticationProvider with class name " + authProviderClass, e);
        }
    }
}
