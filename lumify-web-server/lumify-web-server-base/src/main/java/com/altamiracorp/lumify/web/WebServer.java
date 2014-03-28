package com.altamiracorp.lumify.web;

import com.altamiracorp.lumify.core.cmdline.CommandLineBase;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

public abstract class WebServer extends CommandLineBase {
    private static final String PORT_OPTION_VALUE = "port";
    private static final String HTTPS_PORT_OPTION_VALUE = "httpsPort";
    private static final String KEY_STORE_PATH_OPTION_VALUE = "keyStorePath";
    private static final String KEY_STORE_PASSWORD_OPTION_VALUE = "keyStorePassword";
    private static final int DEFAULT_SERVER_PORT = 8080;
    private static final int DEFAULT_HTTPS_SERVER_PORT = 8443;
    private int httpPort;
    private int httpsPort;
    private String keyStorePath;
    private String keyStorePassword;

    public int getHttpPort() {
        return httpPort;
    }

    public int getHttpsPort() {
        return httpsPort;
    }

    public String getKeyStorePath() {
        return keyStorePath;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    @Override
    protected Options getOptions() {
        final Options options = new Options();

        options.addOption(
                OptionBuilder
                        .withLongOpt(PORT_OPTION_VALUE)
                        .withDescription("The port to run the HTTP connector on")
                        .withArgName("port_number")
                        .hasArg()
                        .create()
        );

        options.addOption(
                OptionBuilder
                        .withLongOpt(HTTPS_PORT_OPTION_VALUE)
                        .withDescription("The port to run the HTTPS connector on")
                        .withArgName("https_port_number")
                        .hasArg()
                        .create()
        );

        options.addOption(
                OptionBuilder
                        .withLongOpt(KEY_STORE_PATH_OPTION_VALUE)
                        .withDescription("Path to the JKS keystore used for SSL")
                        .withArgName("key_store_path")
                        .hasArg()
                        .isRequired()
                        .create()
        );

        options.addOption(
                OptionBuilder
                        .withLongOpt(KEY_STORE_PASSWORD_OPTION_VALUE)
                        .withDescription("JKS keystore password")
                        .withArgName("key_store_password")
                        .hasArg()
                        .isRequired()
                        .create()
        );

        return options;
    }

    @Override
    protected void processOptions(CommandLine cmd) throws Exception {
        keyStorePath = cmd.getOptionValue(KEY_STORE_PATH_OPTION_VALUE);
        keyStorePassword = cmd.getOptionValue(KEY_STORE_PASSWORD_OPTION_VALUE);

        final String insecurePort = cmd.getOptionValue(PORT_OPTION_VALUE);
        if (insecurePort == null) {
            httpPort = DEFAULT_SERVER_PORT;
        } else {
            httpPort = Integer.parseInt(insecurePort);
        }

        final String securePort = cmd.getOptionValue(HTTPS_PORT_OPTION_VALUE);
        if (securePort == null) {
            httpsPort = DEFAULT_HTTPS_SERVER_PORT;
        } else {
            httpsPort = Integer.parseInt(securePort);
        }
    }
}
