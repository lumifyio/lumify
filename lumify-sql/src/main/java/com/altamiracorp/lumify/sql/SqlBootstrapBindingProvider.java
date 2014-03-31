package com.altamiracorp.lumify.sql;

import com.altamiracorp.lumify.core.bootstrap.BootstrapBindingProvider;
import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.exception.LumifyException;
import com.google.inject.Binder;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;

import java.io.File;

public class SqlBootstrapBindingProvider implements BootstrapBindingProvider {
    private static final String HIBERNATE_CFG_XML = "hibernate.cfg.xml";

    @Override
    public void addBindings(Binder binder, Configuration configuration) {
        binder.bind(SessionFactory.class)
                .toProvider(new Provider<SessionFactory>() {
                    @Override
                    public SessionFactory get() {
                        org.hibernate.cfg.Configuration config = new org.hibernate.cfg.Configuration();
                        String fileName = Configuration.CONFIGURATION_LOCATION + HIBERNATE_CFG_XML;
                        if (!(new File(fileName).exists())) {
                            throw new LumifyException("hibernate config missing:" + HIBERNATE_CFG_XML);
                        }
                        config.configure(new File(fileName));
                        ServiceRegistry serviceRegistryBuilder = new StandardServiceRegistryBuilder().applySettings(config.getProperties()).build();
                        SessionFactory sessionFactory = config.buildSessionFactory(serviceRegistryBuilder);
                        return sessionFactory;
                    }
                })
                .in(Scopes.SINGLETON);
    }
}
