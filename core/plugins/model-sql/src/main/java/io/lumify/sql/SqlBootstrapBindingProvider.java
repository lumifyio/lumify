package io.lumify.sql;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Binder;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import io.lumify.core.bootstrap.BootstrapBindingProvider;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.sql.model.HibernateSessionManager;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;

import java.io.File;
import java.util.Set;

public class SqlBootstrapBindingProvider implements BootstrapBindingProvider {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(SqlBootstrapBindingProvider.class);
    private static final String HIBERNATE_CFG_XML = "hibernate.cfg.xml";
    private static final String HIBERNATE_PROPERTY_PREFIX = "hibernate";
    private static final Set<String> OTHER_HIBERNATE_PROPERTIES = ImmutableSet.of("show_sql", "hbm2ddl.auto");

    @Override
    public void addBindings(Binder binder, final Configuration lumifyConfiguration) {
        binder.bind(HibernateSessionManager.class)
                .toProvider(new Provider<HibernateSessionManager>() {
                    @Override
                    public HibernateSessionManager get() {
                        org.hibernate.cfg.Configuration hibernateConfiguration = new org.hibernate.cfg.Configuration();

                        File configFile = lumifyConfiguration.resolveFileName(HIBERNATE_CFG_XML);
                        if (!configFile.exists()) {
                            throw new LumifyException("Hibernate configuration file not found: " + HIBERNATE_CFG_XML);
                        }
                        hibernateConfiguration.configure(configFile);

                        for (String key : lumifyConfiguration.getKeys()) {
                            if (key.startsWith(HIBERNATE_PROPERTY_PREFIX) || OTHER_HIBERNATE_PROPERTIES.contains(key)) {
                                String xmlValue = hibernateConfiguration.getProperty(key);
                                String lumifyValue = lumifyConfiguration.get(key, null);
                                if (lumifyValue != null) {
                                    if (xmlValue == null) {
                                        LOGGER.info("setting Hibernate configuration %s with Lumify configuration value", key);
                                        hibernateConfiguration.setProperty(key, lumifyValue);
                                    } else if (!lumifyValue.equals(xmlValue)) {
                                        LOGGER.info("overriding Hibernate configuration %s with Lumify configuration value", key);
                                        hibernateConfiguration.setProperty(key, lumifyValue);
                                    }
                                }
                            }
                        }

                        ServiceRegistry serviceRegistryBuilder = new StandardServiceRegistryBuilder().applySettings(hibernateConfiguration.getProperties()).build();
                        SessionFactory sessionFactory = hibernateConfiguration.buildSessionFactory(serviceRegistryBuilder);
                        return new HibernateSessionManager(sessionFactory);
                    }
                })
                .in(Scopes.SINGLETON);
    }
}
