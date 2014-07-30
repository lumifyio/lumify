package io.lumify.sql.web;

import com.altamiracorp.miniweb.Handler;
import com.google.inject.Inject;
import io.lumify.sql.model.HibernateSessionManager;
import io.lumify.web.WebApp;
import io.lumify.web.WebAppInitializer;
import io.lumify.web.WebAppPlugin;
import org.hibernate.SessionFactory;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import java.util.EnumSet;

public class SqlModelWebAppPlugin implements WebAppPlugin {
    public static final String FILTER_NAME = "hibernate-session-manager";

    @Inject
    public void configure(SessionFactory sessionFactory) {
        HibernateSessionManager.initialize(sessionFactory);
    }

    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        FilterRegistration.Dynamic filter = servletContext.addFilter(FILTER_NAME, new HibernateSessionManagementFilter());
        filter.addMappingForServletNames(EnumSet.of(DispatcherType.REQUEST), false, WebAppInitializer.SERVLET_NAME);
        filter.setAsyncSupported(true);
    }
}
