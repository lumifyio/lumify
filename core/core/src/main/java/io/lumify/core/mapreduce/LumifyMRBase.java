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
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.CounterGroup;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.util.Tool;
import org.securegraph.accumulo.AccumuloGraphConfiguration;
import org.securegraph.accumulo.mapreduce.AccumuloElementOutputFormat;
import org.securegraph.accumulo.mapreduce.ElementMapper;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public abstract class LumifyMRBase extends Configured implements Tool {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(LumifyMRBase.class);
    public static final String CONFIG_SOURCE_FILE_NAME = "sourceFileName";
    public static final int PERIODIC_COUNTER_OUTPUT_PERIOD = 30 * 1000;
    private String instanceName;
    private String zooKeepers;
    private String principal;
    private AuthenticationToken authorizationToken;
    private boolean local;
    private Timer periodicCounterOutputTimer;

    @Override
    public int run(String[] args) throws Exception {
        io.lumify.core.config.Configuration lumifyConfig = ConfigurationLoader.load();
        JobConf conf = getConfiguration(args, lumifyConfig);
        AccumuloGraphConfiguration accumuloGraphConfiguration = new AccumuloGraphConfiguration(conf, "graph.");
        InjectHelper.inject(this, LumifyBootstrap.bootstrapModuleMaker(lumifyConfig), lumifyConfig);

        Job job = Job.getInstance(conf, getJobName());

        instanceName = accumuloGraphConfiguration.getAccumuloInstanceName();
        zooKeepers = accumuloGraphConfiguration.getZookeeperServers();
        principal = accumuloGraphConfiguration.getAccumuloUsername();
        authorizationToken = accumuloGraphConfiguration.getAuthenticationToken();
        AccumuloElementOutputFormat.setOutputInfo(job, instanceName, zooKeepers, principal, authorizationToken);

        boolean periodicCounterOutput = conf.getBoolean("lumify.periodic.counter.output.enabled", false);

        if (job.getConfiguration().get("mapred.job.tracker").equals("local")) {
            LOGGER.warn("!!!!!! Running in local mode !!!!!!");
            local = true;
            periodicCounterOutput = true;
        }

        setupJob(job);

        if (periodicCounterOutput) {
            startPeriodicCounterOutputThread(job);
        }

        LOGGER.info("Starting job");
        long startTime = System.currentTimeMillis();
        int result = job.waitForCompletion(true) ? 0 : 1;
        long endTime = System.currentTimeMillis();
        LOGGER.info("Job complete");

        if (periodicCounterOutputTimer != null) {
            periodicCounterOutputTimer.cancel();
        }

        printCounters(job);
        LOGGER.info("Time: %,.2f minutes", ((double) (endTime - startTime) / 1000.0 / 60.0));
        LOGGER.info("Return code: " + result);

        return result;
    }

    public boolean isLocal() {
        return local;
    }

    protected void printCounters(Job job) {
        try {
            LOGGER.info("Counters");
            for (String groupName : job.getCounters().getGroupNames()) {
                CounterGroup groupCounters = job.getCounters().getGroup(groupName);
                LOGGER.info(groupCounters.getDisplayName());
                for (Counter counter : groupCounters) {
                    LOGGER.info("  " + counter.getDisplayName() + ": " + counter.getValue());
                }
            }
        } catch (IOException ex) {
            LOGGER.error("Could not print counters", ex);
        }
    }

    protected abstract String getJobName();

    protected abstract void setupJob(Job job) throws Exception;

    protected JobConf getConfiguration(String[] args, io.lumify.core.config.Configuration lumifyConfig) {
        Configuration hadoopConfig = lumifyConfig.toHadoopConfiguration(getConf());
        hadoopConfig.set(ElementMapper.GRAPH_CONFIG_PREFIX, "graph.");
        JobConf result = new JobConf(hadoopConfig, this.getClass());
        parseArgs(result, args);
        setConf(result);
        LOGGER.info("Using config:\n" + result);
        return result;
    }

    protected abstract void parseArgs(JobConf conf, String[] args);

    public String getInstanceName() {
        return instanceName;
    }

    public String getZooKeepers() {
        return zooKeepers;
    }

    public String getPrincipal() {
        return principal;
    }

    public AuthenticationToken getAuthorizationToken() {
        return authorizationToken;
    }

    private void startPeriodicCounterOutputThread(final Job job) {
        periodicCounterOutputTimer = new Timer("periodicCounterOutput", true);
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                printCounters(job);
            }
        };
        periodicCounterOutputTimer.scheduleAtFixedRate(task, PERIODIC_COUNTER_OUTPUT_PERIOD, PERIODIC_COUNTER_OUTPUT_PERIOD);
    }
}
