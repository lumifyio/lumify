package com.altamiracorp.lumify.web;

import com.altamiracorp.lumify.core.FrameworkUtils;
import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.user.SystemUser;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.web.config.ParameterExtractor;
import com.altamiracorp.lumify.web.guice.modules.Bootstrap;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.Properties;

public final class ApplicationBootstrap implements ServletContextListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationBootstrap.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            LOGGER.info("Servlet context initialized...");

            final ServletContext context = sce.getServletContext();

            if (context != null) {
                final Configuration config = fetchApplicationConfiguration(context);
                LOGGER.info("Running application with configuration: " + config);

                final Injector injector = Guice.createInjector(new Bootstrap(config));

                // Store the injector in the context for a servlet to access later
                context.setAttribute(Injector.class.getName(), injector);

                final User user = new SystemUser();
                FrameworkUtils.initializeFramework(injector, user);
            } else {
                LOGGER.error("Servlet context could not be acquired!");
            }
        } catch (Exception ex) {
            LOGGER.error("Failed to initialize context", ex);
            throw new RuntimeException("Failed to initialize context", ex);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOGGER.info("Servlet context destroyed...");
    }


    private Configuration fetchApplicationConfiguration(final ServletContext context) {
        // Extract the required context parameters from the deployment descriptor
        final ParameterExtractor extractor = new ParameterExtractor(context);
        extractor.extractParamAsProperty(Configuration.APP_CONFIG_LOCATION, Configuration.APP_CONFIG_LOCATION);
        extractor.extractParamAsProperty(Configuration.APP_CREDENTIALS_LOCATION, Configuration.APP_CREDENTIALS_LOCATION);

        final Properties appConfigProps = extractor.getApplicationProperties();

        // Find the location of the application configuration and credential files and process them
        final String configLocation = appConfigProps.getProperty(Configuration.APP_CONFIG_LOCATION);
        final String credentialsLocation = appConfigProps.getProperty(Configuration.APP_CREDENTIALS_LOCATION);

        return Configuration.loadConfigurationFile(configLocation, credentialsLocation);
    }
}
