package io.lumify.web;

import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import java.io.File;

public class TomcatWebServer extends WebServer {

    private String webAppDir;
    private String contextPath;

    public static void main(String[] args) throws Exception {
        int res = new TomcatWebServer().run(args);
        if (res != 0) {
            System.exit(res);
        }
    }

    public TomcatWebServer() {
        initFramework = false;
    }

    @Override
    protected Options getOptions() {
        Options options = super.getOptions();

        options.addOption(
                OptionBuilder
                        .withLongOpt("webapp-dir")
                        .withDescription("Path to the webapp directory")
                        .isRequired()
                        .hasArg(true)
                        .create('d')
        );

        options.addOption(
                OptionBuilder
                        .withLongOpt("context-path")
                        .withDescription("Context path for the webapp")
                        .hasArg(true)
                        .create('c')
        );

        return options;
    }

    @Override
    protected void processOptions(CommandLine cmd) throws Exception {
        super.processOptions(cmd);
        this.webAppDir = cmd.getOptionValue('d');
        this.contextPath = cmd.getOptionValue('c', "/lumify");
    }

    @Override
    protected int run(CommandLine cmd) throws Exception {
        Tomcat tomcat = new Tomcat();

        Connector httpsConnector = new Connector();
        httpsConnector.setPort(super.getHttpsPort());
        httpsConnector.setSecure(true);
        httpsConnector.setScheme("https");
        httpsConnector.setAttribute("keystoreFile", super.getKeyStorePath());
        httpsConnector.setAttribute("keystorePass", super.getKeyStorePassword());
        httpsConnector.setAttribute("truststoreFile", super.getTrustStorePath());
        httpsConnector.setAttribute("truststorePass", super.getTrustStorePassword());
        httpsConnector.setAttribute("clientAuth", super.getRequireClientCert() ? "true" : "false");
        httpsConnector.setAttribute("sslProtocol", "TLS");
        httpsConnector.setAttribute("SSLEnabled", true);

        tomcat.setPort(super.getHttpPort());
        tomcat.getService().addConnector(httpsConnector);

        Connector defaultConnector = tomcat.getConnector();
        defaultConnector.setRedirectPort(super.getHttpsPort());

        tomcat.addWebapp(this.contextPath, new File(this.webAppDir).getAbsolutePath());
        System.out.println("configuring app with basedir: " + new File("./" + this.webAppDir).getAbsolutePath());

        tomcat.start();
        tomcat.getServer().await();

        return 0;
    }
}
