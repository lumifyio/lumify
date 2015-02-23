package io.lumify.web.auth;

import io.lumify.core.bootstrap.BootstrapBindingProvider;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.ldap.LdapSearchConfiguration;
import io.lumify.ldap.LdapSearchService;
import io.lumify.ldap.LdapSearchServiceImpl;
import io.lumify.ldap.LdapServerConfiguration;
import com.google.inject.Binder;
import com.google.inject.Provider;
import com.google.inject.Scopes;

public class LdapX509AuthenticationProviderBootstrapBindingProvider implements BootstrapBindingProvider {
    @Override
    public void addBindings(Binder binder, final Configuration configuration) {
        binder.bind(LdapSearchService.class)
                .toProvider(new Provider<LdapSearchService>() {
                    @Override
                    public LdapSearchService get() {
                        LdapServerConfiguration ldapServerConfiguration = new LdapServerConfiguration();
                        configuration.setConfigurables(ldapServerConfiguration, "ldap");

                        LdapSearchConfiguration ldapSearchConfiguration = new LdapSearchConfiguration();
                        configuration.setConfigurables(ldapSearchConfiguration, "ldap");

                        try {
                            return new LdapSearchServiceImpl(ldapServerConfiguration, ldapSearchConfiguration);
                        } catch (Exception e) {
                            throw new LumifyException("failed to configure ldap search service", e);
                        }
                    }
                })
                .in(Scopes.SINGLETON);
    }
}
