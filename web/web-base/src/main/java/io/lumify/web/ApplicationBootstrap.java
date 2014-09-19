package io.lumify.web;

import com.google.inject.Inject;
import com.google.inject.Injector;
import io.lumify.core.FrameworkUtils;
import io.lumify.core.bootstrap.InjectHelper;
import io.lumify.core.bootstrap.LumifyBootstrap;
import io.lumify.core.config.Configuration;
import io.lumify.core.config.ConfigurationLoader;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public final class ApplicationBootstrap implements ServletContextListener {
    private static LumifyLogger LOGGER;
    public static final String APP_CONFIG_LOADER = "application.config.loader";
    public static final String LUMIFY_SERVLET_NAME = "lumify";
    private UserRepository userRepository;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        final ServletContext context = sce.getServletContext();
        System.out.println("Servlet context initialized...");

        if (context != null) {
            final Configuration config = ConfigurationLoader.load(context.getInitParameter(APP_CONFIG_LOADER), getInitParametersAsMap(context));
            LOGGER = LumifyLoggerFactory.getLogger(ApplicationBootstrap.class);
            LOGGER.info("Running application with configuration:\n%s", config);

            setupInjector(context, config);
            setupWebApp(context);
        } else {
            throw new RuntimeException("Failed to initialize context. Lumify is not running.");
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (LOGGER != null) {
            LOGGER.info("Servlet context destroyed...");
        }
    }

    @Inject
    public void setUserRepository(UserRepository userProvider) {
        this.userRepository = userProvider;
    }

    private void setupInjector(ServletContext context, Configuration config) {
        InjectHelper.inject(this, LumifyBootstrap.bootstrapModuleMaker(config));

        // Store the injector in the context for a servlet to access later
        context.setAttribute(Injector.class.getName(), InjectHelper.getInjector());
        if (!config.get(Configuration.MODEL_PROVIDER).equals(Configuration.UNKNOWN_STRING)) {
            FrameworkUtils.initializeFramework(InjectHelper.getInjector(), userRepository.getSystemUser());
        }

        InjectHelper.getInjector().getInstance(OntologyRepository.class);
    }

    private void setupWebApp(ServletContext context) {
        Router router = new Router(context);
        ServletRegistration.Dynamic servlet = context.addServlet(LUMIFY_SERVLET_NAME, router);
        servlet.addMapping("/*");
        servlet.setAsyncSupported(true);
        LOGGER.warn("JavaScript / Less modifications will not be reflected on server. Run `grunt watch` from webapp directory in development");
    }

    private Map<String, String> getInitParametersAsMap(ServletContext context) {
        Map<String, String> initParameters = new HashMap<String, String>();
        Enumeration<String> e = context.getInitParameterNames();
        while (e.hasMoreElements()) {
            String initParameterName = e.nextElement();
            initParameters.put(initParameterName, context.getInitParameter(initParameterName));
        }
        return initParameters;
    }
}
