package com.altamiracorp.lumify.core.cmdline;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.lumify.core.FrameworkUtils;
import com.altamiracorp.lumify.core.bootstrap.InjectHelper;
import com.altamiracorp.lumify.core.bootstrap.LumifyBootstrap;
import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.user.UserProvider;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.google.inject.Inject;
import org.apache.commons.cli.*;
import org.apache.hadoop.fs.FileSystem;
import org.apache.log4j.xml.DOMConfigurator;

import java.io.File;
import java.net.URI;

public abstract class CommandLineBase {
    protected LumifyLogger LOGGER;
    private String configLocation = "/opt/lumify/config/";
    private Configuration configuration;
    private boolean willExit = false;
    protected boolean initFramework = true;
    private UserProvider userProvider;

    public int run(String[] args) throws Exception {
        ensureLoggerInitialized();

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
            }
            processOptions(cmd);
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            printHelp(options);
            return -1;
        }

        if (initFramework) {
            InjectHelper.inject(this, LumifyBootstrap.bootstrapModuleMaker(getConfiguration()));
            FrameworkUtils.initializeFramework(InjectHelper.getInjector(), userProvider.getSystemUser());
        }

        return run(cmd);
    }

    protected void ensureLoggerInitialized() {
        if (LOGGER == null) {
            initLog4j();
        }
    }

    private void initLog4j() {
        String log4jFile = "/opt/lumify/config/log4j.xml";
        if (!new File(log4jFile).exists()) {
            throw new RuntimeException("Could not find log4j configuration at \"" + log4jFile + "\". Did you forget to copy \"docs/log4j.xml.sample\" to \"" + log4jFile + "\"");
        }

        DOMConfigurator.configure(log4jFile);
        LOGGER = LumifyLoggerFactory.getLogger(getClass());
        LOGGER.info("Using log4j.xml: %s", log4jFile);
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
        return this.userProvider.getSystemUser();
    }

    protected boolean willExit() {
        return willExit;
    }

    @Inject
    public void setUserProvider(UserProvider userProvider) {
        this.userProvider = userProvider;
    }

    public UserProvider getUserProvider() {
        return userProvider;
    }
}
