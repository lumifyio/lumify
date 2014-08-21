package io.lumify.web;

import com.google.inject.Inject;
import com.google.inject.Injector;
import io.lumify.core.FrameworkUtils;
import io.lumify.core.bootstrap.InjectHelper;
import io.lumify.core.bootstrap.LumifyBootstrap;
import io.lumify.core.config.Configuration;
import io.lumify.core.config.ConfigurationLoader;
import io.lumify.core.config.FileConfigurationLoader;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public final class ApplicationBootstrap implements ServletContextListener {
    private static LumifyLogger LOGGER;
    public static final String APP_CONFIG_LOADER = "application.config.loader";
    private UserRepository userRepository;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        final ServletContext context = sce.getServletContext();

        System.out.println("Servlet context initialized...");
        try {
            if (context != null) {
                final Configuration config = ConfigurationLoader.load(context.getInitParameter(APP_CONFIG_LOADER), getInitParametersAsMap(context));
                LOGGER = LumifyLoggerFactory.getLogger(ApplicationBootstrap.class);
                LOGGER.info("Running application with configuration:\n%s", config);

                InjectHelper.inject(this, LumifyBootstrap.bootstrapModuleMaker(config));

                // Store the injector in the context for a servlet to access later
                context.setAttribute(Injector.class.getName(), InjectHelper.getInjector());
                if (!config.get(Configuration.MODEL_PROVIDER).equals(Configuration.UNKNOWN_STRING)) {
                    FrameworkUtils.initializeFramework(InjectHelper.getInjector(), userRepository.getSystemUser());
                }

                InjectHelper.getInjector().getInstance(OntologyRepository.class);

                LOGGER.warn("JavaScript / Less modifications will not be reflected on server. Run `grunt watch` from webapp directory in development");
            } else {
                System.out.println("Servlet context could not be acquired!");
            }
        } catch (Exception ex) {
            if (LOGGER != null) {
                LOGGER.error("Failed to initialize context", ex);
            }
            throw new RuntimeException("Failed to initialize context", ex);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (LOGGER != null) {
            LOGGER.info("Servlet context destroyed...");
        }
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

    @Inject
    public void setUserRepository(UserRepository userProvider) {
        this.userRepository = userProvider;
    }
}
