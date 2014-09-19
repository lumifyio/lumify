package io.lumify.web;

import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import java.io.File;

public class TomcatWebServer extends WebServer {

    private Tomcat tomcat;

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
    protected int run(CommandLine cmd) throws Exception {
        tomcat = new Tomcat();

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

        tomcat.addWebapp(this.getContextPath(), new File(this.getWebAppDir()).getAbsolutePath());
        System.out.println("configuring app with basedir: " + new File("./" + this.getWebAppDir()).getAbsolutePath());

        tomcat.start();
        tomcat.getServer().await();

        return 0;
    }

    protected Tomcat getServer() {
        return tomcat;
    }
}
