package com.altamiracorp.lumify.core.cmdline;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.lumify.core.FrameworkUtils;
import com.altamiracorp.lumify.core.bootstrap.InjectHelper;
import com.altamiracorp.lumify.core.bootstrap.LumifyBootstrap;
import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.user.SystemUser;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.securegraph.Authorizations;
import com.altamiracorp.securegraph.Graph;
import com.google.inject.Inject;
import com.netflix.curator.framework.CuratorFramework;
import org.apache.commons.cli.*;
import org.apache.hadoop.fs.FileSystem;

import java.net.URI;

public abstract class CommandLineBase {
    protected static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(CommandLineBase.class);
    private String configLocation = Configuration.CONFIGURATION_LOCATION;
    private Configuration configuration;
    private boolean willExit = false;
    protected boolean initFramework = true;
    private UserRepository userRepository;
    private Authorizations authorizations;
    private User user;
    private CuratorFramework curatorFramework;
    private Graph graph;

    public int run(String[] args) throws Exception {
        final Thread mainThread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                willExit = true;
                try {
                    mainThread.join(1000);
                } catch (InterruptedException e) {
                    // nothing useful to do here
                }
            }
        });

        Options options = getOptions();
        CommandLine cmd;
        try {
            CommandLineParser parser = new GnuParser();
            cmd = parser.parse(options, args);
            if (cmd.hasOption("help")) {
                printHelp(options);
                return 0;
            }
            processOptions(cmd);
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            printHelp(options);
            return -1;
        }

        if (initFramework) {
            InjectHelper.inject(this, LumifyBootstrap.bootstrapModuleMaker(getConfiguration()));
            if (!getConfiguration().get(Configuration.MODEL_PROVIDER).equals(Configuration.UNKNOWN_STRING)) {
                FrameworkUtils.initializeFramework(InjectHelper.getInjector(), userRepository.getSystemUser());
            }
        }

        int result = run(cmd);
        LOGGER.debug("command result: %d", result);

        if (initFramework) {
            shutdown();
        }

        return result;
    }

    protected void shutdown() {
        if (curatorFramework != null) {
            curatorFramework.close();
        }
        if (graph != null) {
            graph.shutdown();
        }
    }

    protected void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("run", options, true);
    }

    protected abstract int run(CommandLine cmd) throws Exception;

    protected void processOptions(CommandLine cmd) throws Exception {
        if (cmd.hasOption("configLocation")) {
            configLocation = cmd.getOptionValue("configLocation");
        }
    }

    protected Options getOptions() {
        Options options = new Options();

        options.addOption(
                OptionBuilder
                        .withLongOpt("help")
                        .withDescription("Print help")
                        .create()
        );

        options.addOption(
                OptionBuilder
                        .withLongOpt("configLocation")
                        .withDescription("Configuration file location")
                        .hasArg()
                        .create()
        );

        return options;
    }

    protected Configuration getConfiguration() {
        if (configuration == null) {
            configuration = Configuration.loadConfigurationFile(configLocation);
        }
        return configuration;
    }

    public ModelUserContext getModelUserContext() {
        return getUser().getModelUserContext();
    }

    protected FileSystem getFileSystem() throws Exception {
        String hdfsRootDir = getConfiguration().get(Configuration.HADOOP_URL);
        org.apache.hadoop.conf.Configuration hadoopConfiguration = new org.apache.hadoop.conf.Configuration();
        return FileSystem.get(new URI(hdfsRootDir), hadoopConfiguration, "hadoop");
    }

    protected Class loadClass(String className) {
        try {
            return this.getClass().getClassLoader().loadClass(className);
        } catch (Exception e) {
            throw new RuntimeException("Could not find class '" + className + "'", e);
        }
    }

    protected User getUser() {
        if (this.user == null) {
            this.user = userRepository.getSystemUser();
        }
        return this.user;
    }

    protected Authorizations getAuthorizations() {
        if (this.authorizations == null) {
            this.authorizations = this.userRepository.getAuthorizations(getUser());
        }
        return this.authorizations;
    }

    protected boolean willExit() {
        return willExit;
    }

    @Inject
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Inject
    public void setCuratorFramework(CuratorFramework curatorFramework) {
        this.curatorFramework = curatorFramework;
    }

    @Inject
    public void setGraph(Graph graph) {
        this.graph = graph;
    }
}
