package com.altamiracorp.lumify.core.cmdline;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.lumify.core.FrameworkUtils;
import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.user.SystemUser;
import com.altamiracorp.lumify.core.user.User;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.commons.cli.*;
import org.apache.hadoop.fs.FileSystem;

import java.net.URI;

public abstract class CommandLineBase {
    private String configLocation = "/opt/lumify/config/";
    private Configuration configuration;
    private User user = new SystemUser();
    private boolean willExit = false;
    protected boolean initFramework = true;

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
            }
            processOptions(cmd);
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            printHelp(options);
            return -1;
        }

        if (initFramework) {
            final Injector injector = Guice.createInjector(CommandLineBootstrap.create(getConfiguration()));
            injector.injectMembers(this);

            final User user = new SystemUser();
            FrameworkUtils.initializeFramework(injector, user);
        }

        return run(cmd);
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
        return user;
    }

    protected boolean willExit() {
        return willExit;
    }
}
