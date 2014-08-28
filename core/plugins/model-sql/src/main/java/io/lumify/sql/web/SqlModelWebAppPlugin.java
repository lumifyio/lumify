package io.lumify.sql.web;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.miniweb.Handler;
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
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(SqlModelWebAppPlugin.class);
    public static final String FILTER_NAME = "hibernate-session-manager";
    private HibernateSessionManager sessionManager;

    @Inject
    public void configure(HibernateSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        FilterRegistration.Dynamic filter = servletContext.addFilter(FILTER_NAME, new HibernateSessionManagementFilter(sessionManager));
        addMapping(filter, WebAppInitializer.SERVLET_NAME);
        addMapping(filter, "AtmosphereServlet");
        // TODO: servletContext.getServletRegistrations().keySet() includes atmosphere but not lumify?
        filter.setAsyncSupported(true);
    }

    private void addMapping(FilterRegistration.Dynamic filter, String servletName) {
        LOGGER.info("mapping %s filter for servlet %s", FILTER_NAME, servletName);
        filter.addMappingForServletNames(EnumSet.of(DispatcherType.REQUEST), false, servletName);
    }
}
