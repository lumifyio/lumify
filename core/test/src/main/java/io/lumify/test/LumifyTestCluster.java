package io.lumify.test;

import io.lumify.core.config.ConfigurationLoader;
import io.lumify.core.config.LumifyTestClusterConfigurationLoader;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

public class LumifyTestCluster {
    private final int httpPort;
    private final int httpsPort;
    private TestAccumulo accumulo;
    private TestElasticSearch elasticsearch;
    private TestJettyServer jetty;
    private Properties config;

    public LumifyTestCluster(int httpPort, int httpsPort) {
        this.httpPort = httpPort;
        this.httpsPort = httpsPort;

        String configLoaderName = System.getenv(ConfigurationLoader.ENV_CONFIGURATION_LOADER);
        if (configLoaderName == null || !configLoaderName.equals(LumifyTestClusterConfigurationLoader.class.getName())) {
            throw new RuntimeException("You must set an environment variable named '"
                    + ConfigurationLoader.ENV_CONFIGURATION_LOADER
                    + "' to the value '"
                    + LumifyTestClusterConfigurationLoader.class.getName()
                    + "' for the Lumify mini cluster to work.");
        }
    }

    public static void main(String[] args) {
        LumifyTestCluster cluster = new LumifyTestCluster(8080, 8443);
        cluster.startup();
    }

    public void startup() {
        config = LumifyTestClusterConfigurationLoader.getConfigurationProperties();
        startAccumulo();
        startElasticSearch();
        startWebServer();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                shutdown();
            }
        });
    }

    public void shutdown() {
        jetty.shutdown();
        elasticsearch.shutdown();
        accumulo.shutdown();
    }

    public Properties getLumifyConfig() {
        return config;
    }

    private void startAccumulo() {
        accumulo = new TestAccumulo(config);
        accumulo.startup();
    }

    private void startElasticSearch() {
        elasticsearch = new TestElasticSearch(config);
        elasticsearch.startup();
    }

    private void startWebServer() {
        try {
            URL keyStoreUrl = LumifyTestCluster.class.getResource("valid.jks");
            String keyStorePath = keyStoreUrl.toURI().getPath();
            jetty = new TestJettyServer(Integer.toString(httpPort), Integer.toString(httpsPort), keyStorePath, "password");
            jetty.startup();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
