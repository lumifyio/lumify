package io.lumify.test;

import org.apache.commons.io.FileUtils;
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class TestElasticSearch {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestElasticSearch.class);
    private static File tempDir;
    private static Node elasticSearchNode;
    private static String addr;
    private final Properties lumifyConfig;

    public TestElasticSearch(Properties config) {
        this.lumifyConfig = config;
    }

    public void startup() {
        try {
            tempDir = File.createTempFile("elasticsearch-temp", Long.toString(System.nanoTime()));
            tempDir.delete();
            tempDir.mkdir();
            LOGGER.info("writing to: " + tempDir);

            elasticSearchNode = NodeBuilder
                    .nodeBuilder()
                    .local(false)
                    .clusterName("elasticsearch")
                    .settings(
                            ImmutableSettings.settingsBuilder()
                                    .put("gateway.type", "local")
                                    .put("path.data", new File(tempDir, "data").getAbsolutePath())
                                    .put("path.logs", new File(tempDir, "logs").getAbsolutePath())
                                    .put("path.work", new File(tempDir, "work").getAbsolutePath())
                    ).node();

            elasticSearchNode.start();

            ClusterStateResponse response = elasticSearchNode.client().admin().cluster().prepareState().execute().actionGet();
            addr = response.getState().getNodes().getNodes().values().iterator().next().value.getAddress().toString();
            addr = addr.substring("inet[/".length());
            addr = addr.substring(0, addr.length() - 1);

            lumifyConfig.setProperty("graph.search.locations", addr);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void shutdown() {
        try {
            if (elasticSearchNode != null) {
                elasticSearchNode.stop();
                elasticSearchNode.close();
            }
            FileUtils.deleteDirectory(tempDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
