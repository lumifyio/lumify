package io.lumify.core.cmdline;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.google.inject.Inject;
import io.lumify.core.FrameworkUtils;
import io.lumify.core.bootstrap.InjectHelper;
import io.lumify.core.bootstrap.LumifyBootstrap;
import io.lumify.core.config.Configuration;
import io.lumify.core.config.ConfigurationLoader;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.commons.cli.*;
import org.apache.curator.framework.CuratorFramework;
import org.apache.hadoop.fs.FileSystem;
import org.securegraph.Authorizations;
import org.securegraph.Graph;

import java.net.URI;

public abstract class CommandLineBase {
    protected static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(CommandLineBase.class);
    private Configuration configuration;
    private boolean willExit = false;
    protected boolean initFramework = true;
    private UserRepository userRepository;
    private Authorizations authorizations;
    private User user;
    private CuratorFramework curatorFramework;
    private Graph graph;
    private WorkQueueRepository workQueueRepository;

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
            if (getConfiguration().get(Configuration.MODEL_PROVIDER, null) != null) {
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
        if (this.curatorFramework != null) {
            LOGGER.debug("shutting down %s", this.curatorFramework.getClass().getName());
            this.curatorFramework.close();
        }
        if (graph != null) {
            LOGGER.debug("shutting down %s", this.graph.getClass().getName());
            this.graph.shutdown();
        }
        if (this.workQueueRepository != null) {
            LOGGER.debug("shutting down %s", this.workQueueRepository.getClass().getName());
            this.workQueueRepository.shutdown();
        }
    }

    protected void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("run", options, true);
    }

    protected abstract int run(CommandLine cmd) throws Exception;

    protected void processOptions(CommandLine cmd) throws Exception {
    }

    protected Options getOptions() {
        Options options = new Options();

        options.addOption(
                OptionBuilder
                        .withLongOpt("help")
                        .withDescription("Print help")
                        .create()
        );

        return options;
    }

    protected Configuration getConfiguration() {
        if (configuration == null) {
            configuration = ConfigurationLoader.load();
        }
        return configuration;
    }

    public ModelUserContext getModelUserContext() {
        return getUser().getModelUserContext();
    }

    protected FileSystem getFileSystem() throws Exception {
        String hdfsRootDir = getConfiguration().get(Configuration.HADOOP_URL, null);
        if (hdfsRootDir == null) {
            throw new LumifyException("Could not find configuration: " + Configuration.HADOOP_URL);
        }
        org.apache.hadoop.conf.Configuration hadoopConfiguration = new org.apache.hadoop.conf.Configuration();
        return FileSystem.get(new URI(hdfsRootDir), hadoopConfiguration, "hadoop");
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
    public final void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Inject
    public final void setGraph(Graph graph) {
        this.graph = graph;
    }

    @Inject
    public final void setWorkQueueRepository(WorkQueueRepository workQueueRepository) {
        this.workQueueRepository = workQueueRepository;
    }

    @Inject
    public final void setCuratorFramework(CuratorFramework curatorFramework) {
        this.curatorFramework = curatorFramework;
    }

    public Graph getGraph() {
        return graph;
    }

    public WorkQueueRepository getWorkQueueRepository() {
        return workQueueRepository;
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }
}
