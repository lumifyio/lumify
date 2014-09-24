package io.lumify.test;

import io.lumify.core.bootstrap.InjectHelper;
import io.lumify.core.config.ConfigurationLoader;
import io.lumify.core.config.LumifyTestClusterConfigurationLoader;
import io.lumify.core.ingest.graphProperty.GraphPropertyRunner;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.Queue;

import static com.google.common.base.Preconditions.checkNotNull;

public class LumifyTestCluster {
    private static LumifyLogger LOGGER;
    private final int httpPort;
    private final int httpsPort;
    private TestAccumulo accumulo;
    private TestElasticSearch elasticsearch;
    private TestJettyServer jetty;
    private Properties config;
    private GraphPropertyRunner graphPropertyRunner;

    public LumifyTestCluster(int httpPort, int httpsPort) {
        this.httpPort = httpPort;
        this.httpsPort = httpsPort;
        System.setProperty(ConfigurationLoader.ENV_CONFIGURATION_LOADER, LumifyTestClusterConfigurationLoader.class.getName());
        LOGGER = LumifyLoggerFactory.getLogger(LumifyTestCluster.class);
    }

    public static void main(String[] args) {
        LumifyTestCluster cluster = new LumifyTestCluster(8080, 8443);
        cluster.startup();
    }

    public void startup() {
        config = LumifyTestClusterConfigurationLoader.getConfigurationProperties();
        setupHdfsFiles();
        startAccumulo();
        startElasticSearch();
        startWebServer();
        setupGraphPropertyRunner();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                shutdown();
            }
        });
    }

    public void setupHdfsFiles() {
        try {
            File hdfsRoot = new File("/tmp/lumify-integration-test");
            File localConfig = new File(getLumifyRootDir(), "./config");
            File hdfsConfig = new File(hdfsRoot, "lumify/config");
            copyFiles(localConfig, hdfsConfig);
        } catch (Exception ex) {
            throw new RuntimeException("Could not setup hdfs files", ex);
        }
    }

    public static File getLumifyRootDir() {
        File startingDir = new File(System.getProperty("user.dir"));
        File f = startingDir;
        while (f != null) {
            if (new File(f, "core").exists() && new File(f, "config").exists()) {
                return f;
            }
            f = f.getParentFile();
        }

        f = new File(startingDir, "lumify-public");
        if (f.exists()) {
            return f;
        }

        throw new RuntimeException("Could not find lumify root starting from " + startingDir.getAbsolutePath());
    }

    private void copyFiles(File sourceDir, File destDir) throws IOException {
        destDir.mkdirs();
        for (File sourceFile : sourceDir.listFiles()) {
            File destFile = new File(destDir, sourceFile.getName());
            if (sourceFile.isDirectory()) {
                copyFiles(sourceFile, destFile);
            } else {
                LOGGER.info("copy file " + sourceFile + " " + destFile);
                FileUtils.copyFile(sourceFile, destFile);
            }
        }
    }

    private void setupGraphPropertyRunner() {
        graphPropertyRunner = InjectHelper.getInstance(GraphPropertyRunner.class);
        graphPropertyRunner.prepare(config);
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
        File keyStoreFile = new File(getLumifyRootDir(), "core/test/src/main/resources/io/lumify/test/valid.jks");
        File webAppDir = new File(getLumifyRootDir(), "web/war/src/main/webapp");
        jetty = new TestJettyServer(webAppDir, httpPort, httpsPort, keyStoreFile.getAbsolutePath(), "password");
        jetty.startup();
    }

    public Queue<JSONObject> getGraphPropertyQueue() {
        return InMemoryWorkQueueRepository.getQueue(WorkQueueRepository.GRAPH_PROPERTY_QUEUE_NAME);
    }

    public void processGraphPropertyQueue() {
        Queue<JSONObject> graphPropertyQueue = getGraphPropertyQueue();
        checkNotNull(graphPropertyQueue, "could not get graphPropertyQueue");
        JSONObject graphPropertyQueueItem;
        while ((graphPropertyQueueItem = graphPropertyQueue.poll()) != null) {
            processGraphPropertyQueueItem(graphPropertyQueueItem);
        }
    }

    private void processGraphPropertyQueueItem(JSONObject graphPropertyQueueItem) {
        try {
            graphPropertyRunner.process(graphPropertyQueueItem);
        } catch (Exception ex) {
            throw new RuntimeException("graphPropertyRunner process", ex);
        }
    }
}
