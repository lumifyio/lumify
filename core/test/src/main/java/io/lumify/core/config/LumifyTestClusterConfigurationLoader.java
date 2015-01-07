package io.lumify.core.config;

import io.lumify.test.LumifyTestCluster;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class LumifyTestClusterConfigurationLoader extends ConfigurationLoader {
    private static Properties props;
    private static boolean usingTestServer = false;

    static {
        loadProps();
    }

    public LumifyTestClusterConfigurationLoader() {
        this(new HashMap());
    }

    public LumifyTestClusterConfigurationLoader(Map initParameters) {
        super(initParameters);
    }

    @Override
    public Configuration createConfiguration() {
        return new Configuration(this, props);
    }

    @Override
    public File resolveFileName(String fileName) {
        File resourceDir = new File(LumifyTestCluster.getLumifyRootDir(), "core/test/src/main/resources/io/lumify/test");
        return new File(resourceDir, fileName);
    }

    public static Properties getConfigurationProperties() {
        return props;
    }

    public static void set(String key, String value) {
        props.put(key, value);
    }

    public static boolean isTestServer() {
        return usingTestServer;
    }

    private static void loadProps() {
        try {
            props = new Properties();
            props.load(LumifyTestClusterConfigurationLoader.class.getResourceAsStream("/io/lumify/test/lumify.properties"));

            String repositoryOntology = System.getProperty("repository.ontology");
            if (repositoryOntology != null && repositoryOntology.length() > 0) {
                props.setProperty("repository.ontology", repositoryOntology);
            }

            String testServer = System.getProperty("testServer");
            if (testServer != null && testServer.length() > 0) {
                usingTestServer = true;
                props.setProperty("hadoop.url", "hdfs://" + testServer + ":8020");
                props.setProperty("zookeeper.serverNames", testServer);

                props.setProperty("bigtable.accumulo.instanceName", "lumify");
                props.setProperty("bigtable.accumulo.zookeeperServerNames", testServer);
                props.setProperty("bigtable.accumulo.username", "root");
                props.setProperty("bigtable.accumulo.password", "password");

                props.setProperty("graph.accumuloInstanceName", "lumify");
                props.setProperty("graph.username", "root");
                props.setProperty("graph.password", "password");
                props.setProperty("graph.tableNamePrefix", "lumify_securegraph");
                props.setProperty("graph.zookeeperServers", testServer);
                props.setProperty("graph.search.locations", testServer);
                props.setProperty("graph.search.indexName", "securegraph");
                props.setProperty("graph.hdfs.rootDir", "hdfs://" + testServer);

                props.setProperty("objectdetection.classifier.face.path", props.getProperty("objectdetection.classifier.face.path").replace("/tmp/lumify-integration-test", ""));
                props.setProperty("termextraction.opennlp.pathPrefix", props.getProperty("termextraction.opennlp.pathPrefix").replace("file:///tmp/lumify-integration-test", ""));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
