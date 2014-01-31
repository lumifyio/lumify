package com.altamiracorp.lumify.web;

import com.altamiracorp.lumify.core.cmdline.CommandLineBase;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;

public class Server extends CommandLineBase {
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

    public static void main(String[] args) throws Exception {
        int res = new Server().run(args);
        if (res != 0) {
            System.exit(res);
        }
    }

    public Server() {
        initFramework = false;
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

    @Override
    protected int run(CommandLine cmd) throws Exception {
        SelectChannelConnector httpConnector = new SelectChannelConnector();
        httpConnector.setPort(httpPort);
        httpConnector.setConfidentialPort(httpsPort);


        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStorePath(keyStorePath);
        sslContextFactory.setKeyStorePassword(keyStorePassword);
        sslContextFactory.setTrustStore(keyStorePath);
        sslContextFactory.setTrustStorePassword(keyStorePassword);
        SslSelectChannelConnector httpsConnector = new SslSelectChannelConnector(sslContextFactory);
        httpsConnector.setPort(httpsPort);

        WebAppContext webAppContext = new WebAppContext();
        webAppContext.setContextPath("/");
        webAppContext.setWar("./lumify-web-war/src/main/webapp/");

        ContextHandlerCollection contexts = new ContextHandlerCollection();
        contexts.setHandlers(new Handler[]{webAppContext});

        org.eclipse.jetty.server.Server server = new org.eclipse.jetty.server.Server();
        server.setConnectors(new Connector[]{httpConnector, httpsConnector});
        server.setHandler(contexts);

        server.start();
        server.join();

        return 0;
    }
}
