package io.lumify.sql.web;

import com.altamiracorp.miniweb.Handler;
import com.google.inject.Inject;
import io.lumify.sql.model.HibernateSessionManager;
import io.lumify.web.WebApp;
import io.lumify.web.WebAppPlugin;
import org.hibernate.SessionFactory;

import javax.servlet.ServletConfig;

public class SqlModelWebAppPlugin implements WebAppPlugin {
    @Inject
    public void configure(SessionFactory sessionFactory) {
        HibernateSessionManager.initialize(sessionFactory);
    }

    @Override
    public void init(WebApp app, ServletConfig config, Handler authenticationHandler) {
        // nothing to do
    }
}
