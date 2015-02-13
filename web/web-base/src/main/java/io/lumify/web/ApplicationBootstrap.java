package io.lumify.web;

import com.altamiracorp.bigtable.model.ModelSession;
import com.google.inject.Inject;
import com.google.inject.Injector;
import io.lumify.core.FrameworkUtils;
import io.lumify.core.bootstrap.InjectHelper;
import io.lumify.core.bootstrap.LumifyBootstrap;
import io.lumify.core.config.Configuration;
import io.lumify.core.config.ConfigurationLoader;
import io.lumify.core.ingest.graphProperty.GraphPropertyRunner;
import io.lumify.core.ingest.video.VideoFrameInfo;
import io.lumify.core.model.longRunningProcess.LongRunningProcessRepository;
import io.lumify.core.model.longRunningProcess.LongRunningProcessRunner;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.termMention.TermMentionRepository;
import io.lumify.core.model.user.AuthorizationRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.core.user.User;
import io.lumify.core.util.GraphUtil;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.atmosphere.cache.UUIDBroadcasterCache;
import org.atmosphere.cpr.AtmosphereHandler;
import org.atmosphere.cpr.AtmosphereInterceptor;
import org.atmosphere.cpr.AtmosphereServlet;
import org.atmosphere.cpr.SessionSupport;
import org.atmosphere.interceptor.HeartbeatInterceptor;
import org.securegraph.Graph;

import javax.servlet.*;
import javax.servlet.annotation.ServletSecurity;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public final class ApplicationBootstrap implements ServletContextListener {
    public static final String CONFIG_HTTP_TRANSPORT_GUARANTEE = "http.transportGuarantee";
    private static LumifyLogger LOGGER;
    public static final String APP_CONFIG_LOADER = "application.config.loader";
    public static final String LUMIFY_SERVLET_NAME = "lumify";
    public static final String ATMOSPHERE_SERVLET_NAME = "atmosphere";
    public static final String DEBUG_FILTER_NAME = "debug";
    public static final String CACHE_FILTER_NAME = "cache";
    private UserRepository userRepository;
    private boolean shouldContinueToRunLongRunningProcess;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        final ServletContext context = sce.getServletContext();
        System.out.println("Servlet context initialized...");

        if (context != null) {
            final Configuration config = ConfigurationLoader.load(context.getInitParameter(APP_CONFIG_LOADER), getInitParametersAsMap(context));
            LOGGER = LumifyLoggerFactory.getLogger(ApplicationBootstrap.class);
            LOGGER.info("Running application with configuration:\n%s", config);

            setupInjector(context, config);
            verifyGraphVersion();
            setupGraphAuthorizations();
            setupWebApp(context, config);
            setupLongRunningProcessRunner(config);
            setupGraphPropertyWorkerRunner(config);
        } else {
            throw new RuntimeException("Failed to initialize context. Lumify is not running.");
        }
    }

    private void verifyGraphVersion() {
        Graph graph = InjectHelper.getInstance(Graph.class);
        GraphUtil.verifyVersion(graph);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        safeLogInfo("BEGIN: Servlet context destroyed...");

        safeLogInfo("Shutdown: ModelSession");
        InjectHelper.getInstance(ModelSession.class).close();

        safeLogInfo("Shutdown: Graph");
        InjectHelper.getInstance(Graph.class).shutdown();

        safeLogInfo("Shutdown: InjectHelper");
        InjectHelper.shutdown();

        safeLogInfo("Shutdown: LumifyBootstrap");
        LumifyBootstrap.shutdown();

        safeLogInfo("END: Servlet context destroyed...");
    }

    private void safeLogInfo(String message) {
        if (LOGGER != null) {
            LOGGER.info("%s", message);
        } else {
            System.out.println(message);
        }
    }

    @Inject
    public void setUserRepository(UserRepository userProvider) {
        this.userRepository = userProvider;
    }

    private void setupInjector(ServletContext context, Configuration config) {
        LOGGER.debug("setupInjector");
        InjectHelper.inject(this, LumifyBootstrap.bootstrapModuleMaker(config), config);

        // Store the injector in the context for a servlet to access later
        context.setAttribute(Injector.class.getName(), InjectHelper.getInjector());
        if (config.get(Configuration.MODEL_PROVIDER, null) != null) {
            FrameworkUtils.initializeFramework(InjectHelper.getInjector(), userRepository.getSystemUser());
        }

        InjectHelper.getInstance(OntologyRepository.class); // verify we are up
    }

    private void setupGraphAuthorizations() {
        LOGGER.debug("setupGraphAuthorizations");
        AuthorizationRepository authorizationRepository = InjectHelper.getInstance(AuthorizationRepository.class);
        authorizationRepository.addAuthorizationToGraph(
                LumifyVisibility.SUPER_USER_VISIBILITY_STRING,
                UserRepository.VISIBILITY_STRING,
                TermMentionRepository.VISIBILITY_STRING,
                LongRunningProcessRepository.VISIBILITY_STRING,
                OntologyRepository.VISIBILITY_STRING,
                WorkspaceRepository.VISIBILITY_STRING,
                VideoFrameInfo.VISIBILITY_STRING
        );
    }

    private void setupWebApp(ServletContext context, Configuration config) {
        LOGGER.debug("setupWebApp");
        Router router = new Router(context);
        ServletRegistration.Dynamic servlet = context.addServlet(LUMIFY_SERVLET_NAME, router);
        servlet.addMapping("/*");
        servlet.setAsyncSupported(true);
        addSecurityConstraint(servlet, config);
        addAtmosphereServlet(context, config);
        addDebugFilter(context);
        addCacheFilter(context);
        LOGGER.warn("JavaScript / Less modifications will not be reflected on server. Run `grunt watch` from webapp directory in development");
    }

    private void addAtmosphereServlet(ServletContext context, Configuration config) {
        ServletRegistration.Dynamic servlet = context.addServlet(ATMOSPHERE_SERVLET_NAME, AtmosphereServlet.class);
        context.addListener(SessionSupport.class);
        servlet.addMapping("/messaging/*");
        servlet.setAsyncSupported(true);
        servlet.setLoadOnStartup(0);
        servlet.setInitParameter(AtmosphereHandler.class.getName(), Messaging.class.getName());
        servlet.setInitParameter("org.atmosphere.cpr.sessionSupport", "true");
        servlet.setInitParameter("org.atmosphere.cpr.broadcastFilterClasses", MessagingFilter.class.getName());
        servlet.setInitParameter(AtmosphereInterceptor.class.getName(), HeartbeatInterceptor.class.getName());
        servlet.setInitParameter("org.atmosphere.interceptor.HeartbeatInterceptor.heartbeatFrequencyInSeconds", "30");
        servlet.setInitParameter("org.atmosphere.cpr.CometSupport.maxInactiveActivity", "-1");
        servlet.setInitParameter("org.atmosphere.cpr.broadcasterCacheClass", UUIDBroadcasterCache.class.getName());
        servlet.setInitParameter("org.atmosphere.websocket.maxTextMessageSize", "1048576");
        servlet.setInitParameter("org.atmosphere.websocket.maxBinaryMessageSize", "1048576");
        addSecurityConstraint(servlet, config);
    }

    private void addDebugFilter(ServletContext context) {
        FilterRegistration.Dynamic filter = context.addFilter(DEBUG_FILTER_NAME, RequestDebugFilter.class);
        filter.setAsyncSupported(true);
        filter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "/*");
    }

    private void addCacheFilter(ServletContext context) {
        FilterRegistration.Dynamic filter = context.addFilter(CACHE_FILTER_NAME, CacheServletFilter.class);
        filter.setAsyncSupported(true);
        String[] mappings = new String[]{"/", "*.html", "*.css", "*.js", "*.ejs", "*.less", "*.hbs", "*.map"};
        for (String mapping : mappings) {
            filter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, mapping);
        }
    }

    private void addSecurityConstraint(ServletRegistration.Dynamic servletRegistration, Configuration config) {
        ServletSecurity.TransportGuarantee transportGuarantee = ServletSecurity.TransportGuarantee.CONFIDENTIAL;
        String constraintType = config.get(CONFIG_HTTP_TRANSPORT_GUARANTEE, null);
        if (constraintType != null) {
            transportGuarantee = ServletSecurity.TransportGuarantee.valueOf(constraintType);
        }

        HttpConstraintElement httpConstraintElement = new HttpConstraintElement(transportGuarantee);
        ServletSecurityElement securityElement = new ServletSecurityElement(httpConstraintElement);
        servletRegistration.setServletSecurity(securityElement);
    }

    private Map<String, String> getInitParametersAsMap(ServletContext context) {
        Map<String, String> initParameters = new HashMap<>();
        Enumeration<String> e = context.getInitParameterNames();
        while (e.hasMoreElements()) {
            String initParameterName = e.nextElement();
            initParameters.put(initParameterName, context.getInitParameter(initParameterName));
        }
        return initParameters;
    }

    private void setupLongRunningProcessRunner(final Configuration config) {
        LOGGER.debug("setupLongRunningProcessRunner");

        boolean enabled = Boolean.parseBoolean(config.get(Configuration.WEB_APP_EMBEDDED_LONG_RUNNING_PROCESS_RUNNER_ENABLED, Boolean.toString(Configuration.WEB_APP_EMBEDDED_LONG_RUNNING_PROCESS_RUNNER_ENABLED_DEFAULT)));
        if (!enabled) {
            LOGGER.debug("skipping embedded long running process runners");
            return;
        }

        int threadCount = Integer.parseInt(config.get(Configuration.WEB_APP_EMBEDDED_LONG_RUNNING_PROCESS_RUNNER_THREAD_COUNT, Integer.toString(Configuration.WEB_APP_EMBEDDED_LONG_RUNNING_PROCESS_RUNNER_THREAD_COUNT_DEFAULT)));

        final LongRunningProcessRunner longRunningProcessRunner = InjectHelper.getInstance(LongRunningProcessRunner.class);
        longRunningProcessRunner.prepare(config.toMap());
        final WorkQueueRepository workQueueRepository = InjectHelper.getInstance(WorkQueueRepository.class);

        LOGGER.debug("long running process runners: %d", threadCount);
        shouldContinueToRunLongRunningProcess = true;
        for (int i = 0; i < threadCount; i++) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    delayStart();
                    while (shouldContinueToRunLongRunningProcess) {
                        WorkQueueRepository.LongRunningProcessMessage longRunningProcessMessage = workQueueRepository.getNextLongRunningProcessMessage();
                        if (longRunningProcessMessage == null) {
                            continue;
                        }
                        try {
                            longRunningProcessRunner.process(longRunningProcessMessage.getMessage());
                            longRunningProcessMessage.complete();
                        } catch (Throwable ex) {
                            LOGGER.error("Failed to process long running process: %s", longRunningProcessMessage.getMessage());
                            longRunningProcessMessage.complete(ex);
                        }
                    }
                }
            });
            t.setName("long-running-process-runner-" + t.getId());
            t.setDaemon(true);
            LOGGER.debug("starting long running process runner thread: %s", t.getName());
            t.start();
        }
    }

    private void setupGraphPropertyWorkerRunner(Configuration config) {
        LOGGER.debug("setupGraphPropertyWorkerRunner");

        boolean enabled = Boolean.parseBoolean(config.get(Configuration.WEB_APP_EMBEDDED_GRAPH_PROPERTY_WORKER_RUNNER_ENABLED, Boolean.toString(Configuration.WEB_APP_EMBEDDED_GRAPH_PROPERTY_WORKER_RUNNER_ENABLED_DEFAULT)));
        if (!enabled) {
            LOGGER.debug("skipping embedded graph property worker");
            return;
        }

        int threadCount = Integer.parseInt(config.get(Configuration.WEB_APP_EMBEDDED_GRAPH_PROPERTY_WORKER_RUNNER_THREAD_COUNT, Integer.toString(Configuration.WEB_APP_EMBEDDED_GRAPH_PROPERTY_WORKER_RUNNER_THREAD_COUNT_DEFAULT)));
        final User user = userRepository.getSystemUser();

        LOGGER.debug("starting graph property worker runners: %d", threadCount);
        for (int i = 0; i < threadCount; i++) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    delayStart();
                    GraphPropertyRunner graphPropertyRunner = InjectHelper.getInstance(GraphPropertyRunner.class);
                    graphPropertyRunner.prepare(user);
                    try {
                        graphPropertyRunner.run();
                    } catch (Exception ex) {
                        LOGGER.error("Failed running graph property runner", ex);
                    }
                }
            });
            t.setName("graph-property-worker-runner-" + t.getId());
            t.setDaemon(true);
            LOGGER.debug("starting graph property worker runner thread: %s", t.getName());
            t.start();
        }
    }

    /**
     * Delay the start of GPW and long running processes so the web app comes up faster
     */
    private void delayStart() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            LOGGER.error("Could not sleep", e);
        }
    }
}
