package io.lumify.reindexmr;

import io.lumify.core.security.LumifyVisibility;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.NullOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.securegraph.GraphFactory;
import org.securegraph.accumulo.AccumuloGraph;
import org.securegraph.accumulo.AccumuloGraphConfiguration;
import org.securegraph.accumulo.mapreduce.AccumuloVertexInputFormat;
import org.securegraph.accumulo.mapreduce.ElementMapper;
import org.securegraph.accumulo.mapreduce.SecureGraphMRUtils;
import org.securegraph.util.MapUtils;

import java.util.Map;

public class ReindexMR extends Configured implements Tool {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ReindexMR.class);

    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new ReindexMR(), args);
        System.exit(res);
    }

    @Override
    public int run(String[] args) throws Exception {
        io.lumify.core.config.Configuration lumifyConfig = io.lumify.core.config.Configuration.loadConfigurationFile();
        Configuration conf = getConfiguration(lumifyConfig);
        AccumuloGraphConfiguration accumuloGraphConfiguration = new AccumuloGraphConfiguration(conf, "graph.");

        Job job = new Job(conf, "lumifyReindex");

        String[] authorizations = new String[]{LumifyVisibility.SUPER_USER_VISIBILITY_STRING};
        Map configurationMap = SecureGraphMRUtils.toMap(conf);
        AccumuloGraph graph = (AccumuloGraph) new GraphFactory().createGraph(MapUtils.getAllWithPrefix(configurationMap, "graph"));
        String instanceName = accumuloGraphConfiguration.getAccumuloInstanceName();
        String zooKeepers = accumuloGraphConfiguration.getZookeeperServers();
        String principal = accumuloGraphConfiguration.getAccumuloUsername();
        AuthenticationToken authorizationToken = accumuloGraphConfiguration.getAuthenticationToken();
        AccumuloVertexInputFormat.setInputInfo(job, graph, instanceName, zooKeepers, principal, authorizationToken, authorizations);

        job.setJarByClass(ReindexMR.class);
        job.setMapperClass(ReindexMRMapper.class);
        job.setInputFormatClass(AccumuloVertexInputFormat.class);
        job.setOutputFormatClass(NullOutputFormat.class);
        return job.waitForCompletion(true) ? 0 : 1;
    }

    private Configuration getConfiguration(io.lumify.core.config.Configuration lumifyConfig) {
        LOGGER.info("Using config:\n" + lumifyConfig);

        Configuration hadoopConfig = lumifyConfig.toHadoopConfiguration();
        hadoopConfig.set(ElementMapper.GRAPH_CONFIG_PREFIX, "graph.");
        this.setConf(hadoopConfig);
        return hadoopConfig;
    }
}
