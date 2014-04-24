package io.lumify.web.guice.modules;

import com.google.inject.Binder;
import io.lumify.core.bootstrap.BootstrapBindingProvider;
import io.lumify.core.config.Configuration;
import io.lumify.web.AuthenticationProvider;

public class LumifyWebBootstrapBindingProvider implements BootstrapBindingProvider {
    @Override
    public void addBindings(final Binder binder, final Configuration configuration) {
        Class<? extends AuthenticationProvider> authProviderClass = configuration.getClass(Configuration.AUTHENTICATION_PROVIDER);
        binder.bind(AuthenticationProvider.class).to(authProviderClass);
    }
}
