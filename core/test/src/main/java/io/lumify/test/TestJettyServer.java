package io.lumify.test;

import io.lumify.web.JettyWebServer;
import io.lumify.web.WebServer;

import java.io.File;

public class TestJettyServer extends JettyWebServer {
    private final File webAppDir;
    private final String httpPort;
    private final String httpsPort;
    private final String keyStorePath;
    private final String keyStorePassword;

    public TestJettyServer(File webAppDir, String httpPort, String httpsPort, String keyStorePath, String keyStorePassword) {
        this.webAppDir = webAppDir;
        this.httpPort = httpPort;
        this.httpsPort = httpsPort;
        this.keyStorePath = keyStorePath;
        this.keyStorePassword = keyStorePassword;
    }

    public void startup() {
        String[] args = new String[] {
                "--" + WebServer.PORT_OPTION_VALUE,
                httpPort,
                "--" + WebServer.HTTPS_PORT_OPTION_VALUE,
                httpsPort,
                "--" + WebServer.KEY_STORE_PATH_OPTION_VALUE,
                keyStorePath,
                "--" + WebServer.KEY_STORE_PASSWORD_OPTION_VALUE,
                keyStorePassword,
                "--" + WebServer.WEB_APP_DIR_OPTION_VALUE,
                webAppDir.getAbsolutePath(),
                "--dontjoin"
        };

        try {
            System.out.println("Running Jetty on http port " + httpPort + ", https port " + httpsPort);
            int code = this.run(args);
            if (code != 0) {
                throw new RuntimeException("Jetty failed to startup");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void shutdown() {
        try {
            if (this.getServer() != null) {
                this.getServer().stop();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
