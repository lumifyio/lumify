package io.lumify.test;

import com.google.common.base.Joiner;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.web.JettyWebServer;
import io.lumify.web.WebServer;

import java.io.File;

import static com.google.common.base.Preconditions.checkNotNull;

public class TestJettyServer extends JettyWebServer {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(TestJettyServer.class);
    private final File webAppDir;
    private final int httpPort;
    private final int httpsPort;
    private final String keyStorePath;
    private final String keyStorePassword;

    public TestJettyServer(File webAppDir, int httpPort, int httpsPort, String keyStorePath, String keyStorePassword) {
        this.webAppDir = webAppDir;
        this.httpPort = httpPort;
        this.httpsPort = httpsPort;
        this.keyStorePath = keyStorePath;
        this.keyStorePassword = keyStorePassword;
    }

    public void startup() {
        checkNotNull(webAppDir, "webAppDir cannot be null");
        checkNotNull(keyStorePath, "keyStorePath cannot be null");
        checkNotNull(keyStorePassword, "keyStorePassword cannot be null");

        String[] args = new String[]{
                "--" + WebServer.PORT_OPTION_VALUE,
                Integer.toString(httpPort),
                "--" + WebServer.HTTPS_PORT_OPTION_VALUE,
                Integer.toString(httpsPort),
                "--" + WebServer.KEY_STORE_PATH_OPTION_VALUE,
                keyStorePath,
                "--" + WebServer.KEY_STORE_PASSWORD_OPTION_VALUE,
                keyStorePassword,
                "--" + WebServer.WEB_APP_DIR_OPTION_VALUE,
                webAppDir.getAbsolutePath(),
                "--dontjoin"
        };

        try {
            LOGGER.info("Running Jetty on http port " + httpPort + ", https port " + httpsPort);
            LOGGER.info("   args: %s", Joiner.on(' ').skipNulls().join(args));
            int code = this.run(args);
            if (code != 0) {
                throw new RuntimeException("Jetty failed to startup");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void shutdown() {
        LOGGER.info("shutdown");
        try {
            if (this.getServer() != null) {
                this.getServer().stop();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
