package com.altamiracorp.lumify.web.guice.modules;

import com.altamiracorp.lumify.core.bootstrap.BootstrapBindingProvider;
import com.altamiracorp.lumify.core.bootstrap.BootstrapUtils;
import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.web.AuthenticationProvider;
import com.google.inject.Binder;

/**
 * This Provider configures the Guice bindings required by the Lumify web application.
 */
public class LumifyWebBootstrapBindingProvider implements BootstrapBindingProvider {
    @Override
    public void addBindings(final Binder binder, final Configuration configuration) {
        Class<? extends AuthenticationProvider> authProviderClass =
                BootstrapUtils.getConfiguredClass(configuration, Configuration.AUTHENTICATION_PROVIDER, true);
        binder.bind(AuthenticationProvider.class).to(authProviderClass);
    }
}
