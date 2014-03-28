package com.altamiracorp.lumify.web;

import com.altamiracorp.lumify.core.FrameworkUtils;
import com.altamiracorp.lumify.core.bootstrap.InjectHelper;
import com.altamiracorp.lumify.core.bootstrap.LumifyBootstrap;
import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.log4j.xml.DOMConfigurator;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.File;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public final class ApplicationBootstrap implements ServletContextListener {
    private static LumifyLogger LOGGER;
    public static final String APP_CONFIG_LOCATION = "application.config.location";
    public static final String APP_LOG4J_LOCATION = "application.config.log4j.location";
    private UserRepository userRepository;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        final ServletContext context = sce.getServletContext();

        initializeLog4j(context);

        LOGGER.info("Servlet context initialized...");
        try {
            if (context != null) {
                final Configuration config = fetchApplicationConfiguration(context);
                LOGGER.info("Running application with configuration:\n%s", config);

                InjectHelper.inject(this, LumifyBootstrap.bootstrapModuleMaker(config));

                // Store the injector in the context for a servlet to access later
                context.setAttribute(Injector.class.getName(), InjectHelper.getInjector());

                FrameworkUtils.initializeFramework(InjectHelper.getInjector(), userRepository.getSystemUser());

                LOGGER.warn("JavaScript / Less modifications will not be reflected on server. Run `grunt watch` from webapp directory in development");
            } else {
                LOGGER.error("Servlet context could not be acquired!");
            }
        } catch (Exception ex) {
            LOGGER.error("Failed to initialize context", ex);
            throw new RuntimeException("Failed to initialize context", ex);
        }
    }

    private void initializeLog4j(final ServletContext context) {
        String log4jFile = context.getInitParameter(APP_LOG4J_LOCATION);
        checkNotNull(log4jFile, "Missing " + APP_LOG4J_LOCATION + " context parameter");
        checkArgument(!log4jFile.isEmpty(), "Empty " + APP_LOG4J_LOCATION + " context parameter");

        if (log4jFile.startsWith("file://")) {
            log4jFile = log4jFile.substring("file://".length());
        }

        if (!new File(log4jFile).exists()) {
            throw new RuntimeException("Could not find log4j configuration at \"" + log4jFile + "\". Did you forget to copy \"docs/log4j.xml.sample\" to \"" + log4jFile + "\"");
        }

        DOMConfigurator.configure(log4jFile);
        LOGGER = LumifyLoggerFactory.getLogger(ApplicationBootstrap.class);
        LOGGER.info("Using log4j.xml: %s", log4jFile);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (LOGGER != null) {
            LOGGER.info("Servlet context destroyed...");
        }
    }

    private Configuration fetchApplicationConfiguration(final ServletContext context) {
        final String configLocation = context.getInitParameter(APP_CONFIG_LOCATION);
        return Configuration.loadConfigurationFile(configLocation);
    }

    @Inject
    public void setUserRepository(UserRepository userProvider) {
        this.userRepository = userProvider;
    }
}
