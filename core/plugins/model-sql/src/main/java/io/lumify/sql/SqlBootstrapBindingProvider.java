package io.lumify.sql;

import com.google.inject.Binder;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import io.lumify.core.bootstrap.BootstrapBindingProvider;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.sql.model.HibernateSessionManager;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;

import java.io.File;

public class SqlBootstrapBindingProvider implements BootstrapBindingProvider {
    private static final String HIBERNATE_CFG_XML = "hibernate.cfg.xml";

    @Override
    public void addBindings(Binder binder, final Configuration configuration) {
        binder.bind(HibernateSessionManager.class)
                .toProvider(new Provider<HibernateSessionManager>() {
                    @Override
                    public HibernateSessionManager get() {
                        org.hibernate.cfg.Configuration config = new org.hibernate.cfg.Configuration();
                        File configFile = configuration.resolveFileName(HIBERNATE_CFG_XML);
                        if (!(configFile.exists())) {
                            throw new LumifyException("hibernate config missing:" + HIBERNATE_CFG_XML);
                        }
                        config.configure(configFile);
                        ServiceRegistry serviceRegistryBuilder = new StandardServiceRegistryBuilder().applySettings(config.getProperties()).build();
                        SessionFactory sessionFactory = config.buildSessionFactory(serviceRegistryBuilder);
                        return new HibernateSessionManager(sessionFactory);
                    }
                })
                .in(Scopes.SINGLETON);
    }
}
