package com.altamiracorp.lumify.cmdline;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;

import com.altamiracorp.lumify.CommandLineBootstrap;
import com.altamiracorp.lumify.FrameworkUtils;
import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.user.ModelAuthorizations;
import com.altamiracorp.lumify.core.user.SystemUser;
import com.altamiracorp.lumify.core.user.User;
import com.google.inject.Guice;
import com.google.inject.Injector;

public abstract class CommandLineBase extends Configured implements Tool {
    private String configLocation = "file:///opt/lumify/config/configuration.properties";
    private String credentialsLocation;
    private Configuration configuration;
    private User user = new SystemUser();
    private boolean willExit = false;
    protected boolean initFramework = true;

    @Override
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

        if( initFramework ) {
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

        if (cmd.hasOption("credentialsLocation")) {
            credentialsLocation = cmd.getOptionValue("credentialsLocation");
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

        options.addOption(
                OptionBuilder
                        .withLongOpt("credentialsLocation")
                        .withDescription("Credentials configuration file location")
                        .hasArg()
                        .create()
        );

        return options;
    }

    protected Configuration getConfiguration() {
        if (configuration == null) {
            configuration = Configuration.loadConfigurationFile(configLocation, credentialsLocation);
        }
        return configuration;
    }

    public ModelUserContext getModelUserContext() {
        return getUser().getModelUserContext();
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
