package io.lumify.reindexmr;

import com.google.inject.Inject;
import io.lumify.core.bootstrap.InjectHelper;
import io.lumify.core.bootstrap.LumifyBootstrap;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
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
import org.securegraph.Graph;
import org.securegraph.accumulo.AccumuloGraph;
import org.securegraph.accumulo.AccumuloGraphConfiguration;
import org.securegraph.accumulo.mapreduce.AccumuloVertexInputFormat;
import org.securegraph.accumulo.mapreduce.ElementMapper;

public class ReindexMR extends Configured implements Tool {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ReindexMR.class);
    private AccumuloGraph graph;

    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new ReindexMR(), args);
        System.exit(res);
    }

    @Override
    public int run(String[] args) throws Exception {
        io.lumify.core.config.Configuration lumifyConfig = io.lumify.core.config.Configuration.loadConfigurationFile();
        Configuration conf = getConfiguration(args, lumifyConfig);
        AccumuloGraphConfiguration accumuloGraphConfiguration = new AccumuloGraphConfiguration(conf, "graph.");
        InjectHelper.inject(this, LumifyBootstrap.bootstrapModuleMaker(lumifyConfig));

        Job job = new Job(conf, "lumifyReindex");

        String[] authorizations = new String[]{
                LumifyVisibility.SUPER_USER_VISIBILITY_STRING,
                OntologyRepository.VISIBILITY_STRING,
                UserRepository.VISIBILITY_STRING,
                WorkspaceRepository.VISIBILITY_STRING
        };
        String instanceName = accumuloGraphConfiguration.getAccumuloInstanceName();
        String zooKeepers = accumuloGraphConfiguration.getZookeeperServers();
        String principal = accumuloGraphConfiguration.getAccumuloUsername();
        AuthenticationToken authorizationToken = accumuloGraphConfiguration.getAuthenticationToken();
        AccumuloVertexInputFormat.setInputInfo(job, graph, instanceName, zooKeepers, principal, authorizationToken, authorizations);

        job.setJarByClass(ReindexMR.class);
        job.setMapperClass(ReindexMRMapper.class);
        job.setInputFormatClass(AccumuloVertexInputFormat.class);
        job.setOutputFormatClass(NullOutputFormat.class);
        job.setNumReduceTasks(0);
        return job.waitForCompletion(true) ? 0 : 1;
    }

    private Configuration getConfiguration(String[] args, io.lumify.core.config.Configuration lumifyConfig) {
        LOGGER.info("Using config:\n" + lumifyConfig);
        Configuration hadoopConfig = lumifyConfig.toHadoopConfiguration();
        hadoopConfig.set(ElementMapper.GRAPH_CONFIG_PREFIX, "graph.");
        this.setConf(hadoopConfig);
        return hadoopConfig;
    }

    @Inject
    public void setGraph(Graph graph) {
        this.graph = (AccumuloGraph) graph;
    }
}
