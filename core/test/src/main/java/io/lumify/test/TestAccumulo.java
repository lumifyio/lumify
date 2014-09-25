package io.lumify.test;

import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.minicluster.MiniAccumuloCluster;
import org.apache.accumulo.minicluster.MiniAccumuloConfig;
import org.securegraph.accumulo.AccumuloGraphConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class TestAccumulo {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestAccumulo.class);
    private static final String ACCUMULO_USERNAME = "root";
    private static final String ACCUMULO_PASSWORD = "test";
    private final Properties lumifyConfig;
    private File tempDir;
    private MiniAccumuloCluster accumulo;
    private AccumuloGraphConfiguration config;
    private Connector connector;

    public TestAccumulo(Properties config) {
        this.lumifyConfig = config;
    }

    public void startup() {
        try {
            tempDir = File.createTempFile("accumulo-test", Long.toString(System.nanoTime()));
            tempDir.delete();
            tempDir.mkdir();
            LOGGER.info("writing to: " + tempDir);

            LOGGER.info("Starting accumulo");
            MiniAccumuloConfig miniAccumuloConfig = new MiniAccumuloConfig(tempDir, ACCUMULO_PASSWORD);
            accumulo = new MiniAccumuloCluster(miniAccumuloConfig);
            accumulo.start();

            setLumifyProperties();

            Map configMap = getConfig();
            config = new AccumuloGraphConfiguration(configMap);
            connector = config.createConnector();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void shutdown() {
        LOGGER.info("shutdown");
        try {
            if (accumulo != null) {
                accumulo.stop();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            tempDir.delete();
            config = null;
            accumulo = null;
        }
    }

    public void setAuthorizations(String user, String... auths) {
        try {
            connector.securityOperations().changeUserAuthorizations(user, new Authorizations(auths));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected Map<String, Object> getConfig() {
        Map<String, Object> configMap = new HashMap<String, Object>();
        configMap.put(AccumuloGraphConfiguration.ZOOKEEPER_SERVERS, accumulo.getZooKeepers());
        configMap.put(AccumuloGraphConfiguration.ACCUMULO_INSTANCE_NAME, accumulo.getInstanceName());
        configMap.put(AccumuloGraphConfiguration.ACCUMULO_USERNAME, ACCUMULO_USERNAME);
        configMap.put(AccumuloGraphConfiguration.ACCUMULO_PASSWORD, ACCUMULO_PASSWORD);
        configMap.put(AccumuloGraphConfiguration.AUTO_FLUSH, true);
        configMap.put(AccumuloGraphConfiguration.DATA_DIR, "/tmp/");
        return configMap;
    }

    private void setLumifyProperties() {
        lumifyConfig.setProperty("bigtable.accumulo.instanceName", accumulo.getInstanceName());
        lumifyConfig.setProperty("bigtable.accumulo.zookeeperServerNames", accumulo.getZooKeepers());
        lumifyConfig.setProperty("graph.zookeeperServers", accumulo.getZooKeepers());
    }
}
