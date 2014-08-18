package io.lumify.core.mapreduce;

import io.lumify.core.bootstrap.InjectHelper;
import io.lumify.core.bootstrap.LumifyBootstrap;
import io.lumify.core.config.ConfigurationLoader;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.util.Tool;
import org.securegraph.accumulo.AccumuloGraphConfiguration;
import org.securegraph.accumulo.mapreduce.AccumuloElementOutputFormat;
import org.securegraph.accumulo.mapreduce.ElementMapper;

import java.io.File;

public abstract class LumifyMRBase extends Configured implements Tool {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(LumifyMRBase.class);
    public static final String CONFIG_SOURCE_FILE_NAME = "sourceFileName";

    @Override
    public int run(String[] args) throws Exception {
        io.lumify.core.config.Configuration lumifyConfig = ConfigurationLoader.load();
        JobConf conf = getConfiguration(args, lumifyConfig);
        AccumuloGraphConfiguration accumuloGraphConfiguration = new AccumuloGraphConfiguration(conf, "graph.");
        InjectHelper.inject(this, LumifyBootstrap.bootstrapModuleMaker(lumifyConfig));

        Job job = new Job(conf, getJobName());

        String instanceName = accumuloGraphConfiguration.getAccumuloInstanceName();
        String zooKeepers = accumuloGraphConfiguration.getZookeeperServers();
        String principal = accumuloGraphConfiguration.getAccumuloUsername();
        AuthenticationToken authorizationToken = accumuloGraphConfiguration.getAuthenticationToken();
        AccumuloElementOutputFormat.setOutputInfo(job, instanceName, zooKeepers, principal, authorizationToken);

        if (job.getConfiguration().get("mapred.job.tracker").equals("local")) {
            LOGGER.warn("!!!!!! Running in local mode !!!!!!");
        }

        return run(job);
    }

    protected abstract String getJobName();

    protected abstract int run(Job job) throws Exception;

    protected JobConf getConfiguration(String[] args, io.lumify.core.config.Configuration lumifyConfig) {
        if (args.length != 1) {
            throw new RuntimeException("Required arguments <inputFileName>");
        }
        String inFileName = args[0];
        LOGGER.info("Using config:\n" + lumifyConfig);

        Configuration hadoopConfig = lumifyConfig.toHadoopConfiguration(getConf());
        hadoopConfig.set(ElementMapper.GRAPH_CONFIG_PREFIX, "graph.");
        LOGGER.info("inFileName: %s", inFileName);
        hadoopConfig.set("in", inFileName);
        hadoopConfig.set(CONFIG_SOURCE_FILE_NAME, new File(inFileName).getName());
        JobConf result = new JobConf(hadoopConfig, this.getClass());
        setConf(result);
        return result;
    }
}
