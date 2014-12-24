package io.lumify.web;

import io.lumify.core.cmdline.CommandLineBase;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

public abstract class WebServer extends CommandLineBase {
    public static final String PORT_OPTION_VALUE = "port";
    public static final String HTTPS_PORT_OPTION_VALUE = "httpsPort";
    public static final String KEY_STORE_PATH_OPTION_VALUE = "keyStorePath";
    public static final String KEY_STORE_PASSWORD_OPTION_VALUE = "keyStorePassword";
    public static final String TRUST_STORE_PATH_OPTION_VALUE = "trustStorePath";
    public static final String TRUST_STORE_PASSWORD_OPTION_VALUE = "trustStorePassword";
    public static final String REQUIRE_CLIENT_CERT_OPTION_VALUE = "requireClientCert";
    public static final String WEB_APP_DIR_OPTION_VALUE = "webAppDir";
    public static final String CONTEXT_PATH_OPTION_VALUE = "contextPath";
    public static final String SESSION_TIMEOUT_OPTION_VALUE = "sessionTimeout";
    public static final int DEFAULT_SERVER_PORT = 8080;
    public static final int DEFAULT_HTTPS_SERVER_PORT = 8443;
    public static final String DEFAULT_CONTEXT_PATH = "/";
    public static final int DEFAULT_SESSION_TIMEOUT = 30;

    private int httpPort;
    private int httpsPort;
    private String keyStorePath;
    private String keyStorePassword;
    private String trustStorePath;
    private String trustStorePassword;
    private boolean requireClientCert = false;
    private String webAppDir;
    private String contextPath;
    private int sessionTimeout;

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

    public String getTrustStorePath() {
        if (trustStorePath != null && trustStorePath.trim().length() > 0) {
            return trustStorePath;
        } else {
            return keyStorePath;
        }
    }

    public String getTrustStorePassword() {
        if (trustStorePassword != null && trustStorePassword.trim().length() > 0) {
            return trustStorePassword;
        } else {
            return keyStorePassword;
        }
    }

    public boolean getRequireClientCert() {
        return requireClientCert;
    }

    public String getContextPath() {
        return contextPath;
    }

    public String getWebAppDir() {
        return webAppDir;
    }

    public int getSessionTimeout() {
        return sessionTimeout;
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

        options.addOption(
                OptionBuilder
                        .withLongOpt(TRUST_STORE_PATH_OPTION_VALUE)
                        .withDescription("Path to the JKS truststore used for SSL")
                        .withArgName("trust_store_path")
                        .hasArg()
                        .create()
        );

        options.addOption(
                OptionBuilder
                        .withLongOpt(TRUST_STORE_PASSWORD_OPTION_VALUE)
                        .withDescription("JKS truststore password")
                        .withArgName("trust_store_password")
                        .hasArg()
                        .create()
        );

        options.addOption(
                OptionBuilder
                        .withLongOpt(REQUIRE_CLIENT_CERT_OPTION_VALUE)
                        .withDescription("require client certificate")
                        .create()
        );

        options.addOption(
                OptionBuilder
                        .withLongOpt(WEB_APP_DIR_OPTION_VALUE)
                        .withDescription("Path to the webapp directory")
                        .isRequired()
                        .hasArg(true)
                        .create()
        );

        options.addOption(
                OptionBuilder
                        .withLongOpt(CONTEXT_PATH_OPTION_VALUE)
                        .withDescription("Context path for the webapp")
                        .hasArg(true)
                        .create()
        );

        options.addOption(
                OptionBuilder
                        .withLongOpt(SESSION_TIMEOUT_OPTION_VALUE)
                        .withDescription("number of minutes before idle sessions expire")
                        .hasArg(true)
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

        trustStorePath = cmd.getOptionValue(TRUST_STORE_PATH_OPTION_VALUE);
        trustStorePassword = cmd.getOptionValue(TRUST_STORE_PASSWORD_OPTION_VALUE);

        requireClientCert = cmd.hasOption(REQUIRE_CLIENT_CERT_OPTION_VALUE);

        webAppDir = cmd.getOptionValue(WEB_APP_DIR_OPTION_VALUE);
        contextPath = cmd.getOptionValue(CONTEXT_PATH_OPTION_VALUE, DEFAULT_CONTEXT_PATH);

        final String timeout = cmd.getOptionValue(SESSION_TIMEOUT_OPTION_VALUE);
        if (timeout == null) {
            sessionTimeout = DEFAULT_SESSION_TIMEOUT;
        } else {
            sessionTimeout = Integer.parseInt(timeout);
        }
    }
}
