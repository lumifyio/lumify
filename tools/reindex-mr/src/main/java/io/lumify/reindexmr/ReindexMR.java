package io.lumify.reindexmr;

import com.google.inject.Inject;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.mapreduce.LumifyMRBase;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.core.util.GraphUtil;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.NullOutputFormat;
import org.apache.hadoop.util.ToolRunner;
import org.securegraph.ElementType;
import org.securegraph.Graph;
import org.securegraph.accumulo.AccumuloGraph;
import org.securegraph.accumulo.mapreduce.AccumuloEdgeInputFormat;
import org.securegraph.accumulo.mapreduce.AccumuloVertexInputFormat;

public class ReindexMR extends LumifyMRBase {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ReindexMR.class);
    private AccumuloGraph graph;
    private ElementType elementType;

    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new ReindexMR(), args);
        System.exit(res);
    }

    @Override
    protected void setupJob(Job job) throws Exception {
        String[] authorizations = new String[]{
                LumifyVisibility.SUPER_USER_VISIBILITY_STRING,
                OntologyRepository.VISIBILITY_STRING,
                UserRepository.VISIBILITY_STRING,
                WorkspaceRepository.VISIBILITY_STRING,
                GraphUtil.SOURCE_VISIBILITY_STRING
        };

        job.getConfiguration().setBoolean("mapred.map.tasks.speculative.execution", false);
        job.getConfiguration().setBoolean("mapred.reduce.tasks.speculative.execution", false);

        job.setJarByClass(ReindexMR.class);
        job.setMapperClass(ReindexMRMapper.class);
        job.setOutputFormatClass(NullOutputFormat.class);
        job.setNumReduceTasks(0);

        if (elementType == ElementType.VERTEX) {
            job.setInputFormatClass(AccumuloVertexInputFormat.class);
            AccumuloVertexInputFormat.setInputInfo(job, graph, getInstanceName(), getZooKeepers(), getPrincipal(), getAuthorizationToken(), authorizations);
        } else if (elementType == ElementType.EDGE) {
            job.setInputFormatClass(AccumuloEdgeInputFormat.class);
            AccumuloEdgeInputFormat.setInputInfo(job, graph, getInstanceName(), getZooKeepers(), getPrincipal(), getAuthorizationToken(), authorizations);
        } else {
            throw new LumifyException("Unhandled element type: " + elementType);
        }
    }

    @Override
    protected void parseArgs(JobConf conf, String[] args) {
        if (args.length != 1) {
            throw new RuntimeException("Required arguments <vertex|edge>");
        }
        elementType = ElementType.valueOf(args[0].toUpperCase());
        LOGGER.info("Element type: " + elementType);
    }

    @Override
    protected String getJobName() {
        return "lumifyReindex-" + elementType;
    }

    @Inject
    public void setGraph(Graph graph) {
        this.graph = (AccumuloGraph) graph;
    }
}
