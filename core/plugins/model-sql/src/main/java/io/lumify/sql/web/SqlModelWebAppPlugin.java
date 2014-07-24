package io.lumify.sql.web;

import com.altamiracorp.miniweb.Handler;
import com.google.inject.Inject;
import io.lumify.web.WebApp;
import io.lumify.web.WebAppPlugin;
import org.hibernate.SessionFactory;

import javax.servlet.DispatcherType;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.util.EnumSet;

public class SqlModelWebAppPlugin implements WebAppPlugin {

    private SessionFactory sessionFactory;

    @Inject
    public void configure(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void init(WebApp app, ServletConfig config, Handler authenticationHandler) {
        ServletContext context = config.getServletContext();
        HibernateSessionManagementFilter sessionManagerFilter = new HibernateSessionManagementFilter(this.sessionFactory);
        context.addFilter("HibernateSessionManagementFilter", sessionManagerFilter)
                .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
    }
}
