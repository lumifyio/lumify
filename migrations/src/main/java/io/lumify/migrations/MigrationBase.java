package io.lumify.migrations;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import io.lumify.core.bootstrap.InjectHelper;
import io.lumify.core.bootstrap.LumifyBootstrap;
import io.lumify.core.config.Configuration;
import io.lumify.core.config.ConfigurationLoader;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.core.util.GraphUtil;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.securegraph.Graph;
import org.securegraph.accumulo.AccumuloGraph;
import org.securegraph.accumulo.AccumuloGraphConfiguration;
import org.securegraph.accumulo.mapreduce.AccumuloEdgeInputFormat;
import org.securegraph.accumulo.mapreduce.AccumuloElementOutputFormat;
import org.securegraph.accumulo.mapreduce.AccumuloVertexInputFormat;
import org.securegraph.accumulo.mapreduce.ElementMapper;

import java.io.IOException;
import java.util.HashMap;

public abstract class MigrationBase extends Configured implements Tool {
    private static LumifyLogger LOGGER;
    private static final String CONFIG_DRY_RUN = "migration.dryrun";
    private final AccumuloGraph graph;
    private final Configuration configuration;

    @Parameter(names = {"-force"}, description = "Force the migration.")
    private boolean forceMigration = false;

    @Parameter(names = {"-dryrun", "-dry-run"}, description = "Print the modifications, don't perform them.")
    private boolean dryRun = false;

    protected MigrationBase(
            Graph graph,
            Configuration configuration
    ) {
        this.graph = (AccumuloGraph) graph;
        this.configuration = configuration;
    }

    protected static int run(Class<? extends MigrationBase> migrationClass, String[] args) throws Exception {
        Configuration config = ConfigurationLoader.load(new HashMap());
        LOGGER = LumifyLoggerFactory.getLogger(migrationClass); // must initialize logger after configuration loaded
        MigrationBase migrationBase = InjectHelper.getInstance(migrationClass, LumifyBootstrap.bootstrapModuleMaker(config), config);
        return ToolRunner.run(new org.apache.hadoop.conf.Configuration(), migrationBase, args);
    }

    @Override
    public int run(String[] args) throws Exception {
        new JCommander(this, args);
        try {
            verifyVersion(forceMigration);
        } catch (LumifyException ex) {
            throw new LumifyException("Could not verify version. Run with -force option to force the migraion.", ex);
        }

        AccumuloGraphConfiguration accumuloGraphConfiguration = new AccumuloGraphConfiguration(getConfiguration().toHadoopConfiguration(), "graph.");
        Job migrateVerticesJob = migrateVertices(accumuloGraphConfiguration);
        Job migrateEdgesJob = migrateEdges(accumuloGraphConfiguration);

        migrateVerticesJob.waitForCompletion(true);
        migrateEdgesJob.waitForCompletion(true);

        LOGGER.info("Vertex Counters");
        printCounters(migrateVerticesJob);

        LOGGER.info("Edge Counters");
        printCounters(migrateEdgesJob);

        if (!migrateVerticesJob.isSuccessful()) {
            throw new LumifyException("Failed to migrate vertices");
        }
        if (!migrateEdgesJob.isSuccessful()) {
            throw new LumifyException("Failed to migrate edges");
        }

        writeNewVersion();
        return 0;
    }

    protected Job migrateVertices(AccumuloGraphConfiguration accumuloGraphConfiguration) throws Exception {
        return migrate(accumuloGraphConfiguration, MigrationType.VERTICES, getVertexMigrationMapperClass());
    }

    protected abstract Class<? extends ElementMigrationMapperBase> getVertexMigrationMapperClass();

    protected Job migrateEdges(AccumuloGraphConfiguration accumuloGraphConfiguration) throws Exception {
        return migrate(accumuloGraphConfiguration, MigrationType.EDGES, getEdgeMigrationMapperClass());
    }

    protected Class<? extends ElementMigrationMapperBase> getEdgeMigrationMapperClass() {
        return getVertexMigrationMapperClass();
    }

    protected Job migrate(AccumuloGraphConfiguration accumuloGraphConfiguration, MigrationType migrationType, Class<? extends ElementMigrationMapperBase> mapperClass) throws IOException, AccumuloSecurityException, InterruptedException, ClassNotFoundException {
        String jobNameSuffix;
        Class<? extends InputFormat> inputFormatClass;
        switch (migrationType) {
            case VERTICES:
                jobNameSuffix = "migrateVertices";
                inputFormatClass = AccumuloVertexInputFormat.class;
                break;
            case EDGES:
                jobNameSuffix = "migrateEdges";
                inputFormatClass = AccumuloEdgeInputFormat.class;
                break;
            default:
                throw new LumifyException("Invalid migration type: " + migrationType);
        }

        JobConf jobConf = getJobConf(getConfiguration());
        String jobName = getClass().getSimpleName() + "-" + jobNameSuffix;
        Job job = Job.getInstance(jobConf, jobName);
        job.setJarByClass(getClass());

        String instanceName = accumuloGraphConfiguration.getAccumuloInstanceName();
        String zooKeepers = accumuloGraphConfiguration.getZookeeperServers();
        String principal = accumuloGraphConfiguration.getAccumuloUsername();
        AuthenticationToken authorizationToken = accumuloGraphConfiguration.getAuthenticationToken();
        AccumuloElementOutputFormat.setOutputInfo(job, instanceName, zooKeepers, principal, authorizationToken);
        job.setOutputFormatClass(AccumuloElementOutputFormat.class);

        if (inputFormatClass == AccumuloVertexInputFormat.class) {
            AccumuloVertexInputFormat.setInputInfo(job, graph, instanceName, zooKeepers, principal, authorizationToken, getAuthorizations());
        } else if (inputFormatClass == AccumuloEdgeInputFormat.class) {
            AccumuloEdgeInputFormat.setInputInfo(job, graph, instanceName, zooKeepers, principal, authorizationToken, getAuthorizations());
        }
        job.setInputFormatClass(inputFormatClass);

        job.setMapperClass(mapperClass);
        job.setNumReduceTasks(0);

        job.submit();
        return job;
    }

    protected String[] getAuthorizations() {
        return new String[]{
                LumifyVisibility.SUPER_USER_VISIBILITY_STRING,
                OntologyRepository.VISIBILITY_STRING,
                UserRepository.VISIBILITY_STRING,
                WorkspaceRepository.VISIBILITY_STRING,
                GraphUtil.SOURCE_VISIBILITY_STRING
        };
    }

    protected JobConf getJobConf(Configuration lumifyConfig) {
        org.apache.hadoop.conf.Configuration hadoopConfig = lumifyConfig.toHadoopConfiguration(getConf());
        hadoopConfig.set(ElementMapper.GRAPH_CONFIG_PREFIX, "graph.");
        hadoopConfig.set(CONFIG_DRY_RUN, Boolean.toString(dryRun));
        JobConf result = new JobConf(hadoopConfig, this.getClass());
        setConf(result);
        LOGGER.info("Using config:\n" + result);
        return result;
    }

    protected void writeNewVersion() {
        if (dryRun) {
            LOGGER.debug("dry-run: writing version: %d", getToVersion());
        } else {
            GraphUtil.writeVersion(getGraph(), getToVersion());
        }
    }

    protected void verifyVersion(boolean forceMigration) {
        if (forceMigration) {
            try {
                GraphUtil.verifyVersion(getGraph(), getFromVersion());
            } catch (LumifyException ex) {
                LOGGER.warn("Version verification failed: %s (continuing because of force)", ex.getMessage());
            }
        } else {
            GraphUtil.verifyVersion(getGraph(), getFromVersion());
        }
    }

    protected abstract int getFromVersion();

    protected int getToVersion() {
        return getFromVersion() + 1;
    }

    public Graph getGraph() {
        return graph;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    protected void printCounters(Job job) {
        try {
            for (String groupName : job.getCounters().getGroupNames()) {
                CounterGroup groupCounters = job.getCounters().getGroup(groupName);
                LOGGER.info("  " + groupCounters.getDisplayName());
                for (Counter counter : groupCounters) {
                    LOGGER.info("    " + counter.getDisplayName() + ": " + counter.getValue());
                }
            }
        } catch (IOException ex) {
            LOGGER.error("Could not print counters", ex);
        }
    }

    public static boolean isDryRun(Mapper.Context context) {
        String dryRunStr = context.getConfiguration().get(CONFIG_DRY_RUN);
        if (dryRunStr == null) {
            return false;
        }
        return Boolean.parseBoolean(dryRunStr);
    }

    protected static enum MigrationType {
        VERTICES,
        EDGES
    }
}
