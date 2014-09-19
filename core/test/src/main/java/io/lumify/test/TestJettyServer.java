package io.lumify.test;

import io.lumify.web.JettyWebServer;
import io.lumify.web.WebServer;

public class TestJettyServer extends JettyWebServer {
    private final String httpPort;
    private final String httpsPort;
    private final String keyStorePath;
    private final String keyStorePassword;

    public TestJettyServer(String httpPort, String httpsPort, String keyStorePath, String keyStorePassword) {
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
                "web/war/src/main/webapp",
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
