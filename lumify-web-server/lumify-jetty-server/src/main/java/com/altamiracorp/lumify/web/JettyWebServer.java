package com.altamiracorp.lumify.web;

import org.apache.commons.cli.CommandLine;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;

public class JettyWebServer extends WebServer {

    public static void main(String[] args) throws Exception {
        int res = new JettyWebServer().run(args);
        if (res != 0) {
            System.exit(res);
        }
    }

    public JettyWebServer() {
        initFramework = false;
    }

    @Override
    protected int run(CommandLine cmd) throws Exception {
        SelectChannelConnector httpConnector = new SelectChannelConnector();
        httpConnector.setPort(super.getHttpPort());
        httpConnector.setConfidentialPort(super.getHttpsPort());

        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStorePath(super.getKeyStorePath());
        sslContextFactory.setKeyStorePassword(super.getKeyStorePassword());
        sslContextFactory.setTrustStore(super.getTrustStorePath());
        sslContextFactory.setTrustStorePassword(super.getTrustStorePassword());
        sslContextFactory.setNeedClientAuth(super.getRequireClientCert());
        SslSelectChannelConnector httpsConnector = new SslSelectChannelConnector(sslContextFactory);
        httpsConnector.setPort(super.getHttpsPort());

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
