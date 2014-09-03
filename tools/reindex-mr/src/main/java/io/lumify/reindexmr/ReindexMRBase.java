package io.lumify.reindexmr;

import com.google.inject.Inject;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.mapreduce.LumifyMRBase;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.security.LumifyVisibility;
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

public abstract class ReindexMRBase extends LumifyMRBase {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ReindexMRBase.class);
    private AccumuloGraph graph;
    protected ElementType elementType;

    @Override
    protected void setupJob(Job job) throws Exception {
        String[] authorizations = new String[]{
                LumifyVisibility.SUPER_USER_VISIBILITY_STRING,
                OntologyRepository.VISIBILITY_STRING,
                UserRepository.VISIBILITY_STRING,
                WorkspaceRepository.VISIBILITY_STRING
        };

        job.setJarByClass(ReindexMRBase.class);
        setupJobMapper(job);
        job.setNumReduceTasks(0);

        if (elementType == ElementType.VERTEX) {
            AccumuloVertexInputFormat.setInputInfo(job, graph, getInstanceName(), getZooKeepers(), getPrincipal(), getAuthorizationToken(), authorizations);
            job.setInputFormatClass(AccumuloVertexInputFormat.class);
        } else if (elementType == ElementType.EDGE) {
            AccumuloEdgeInputFormat.setInputInfo(job, graph, getInstanceName(), getZooKeepers(), getPrincipal(), getAuthorizationToken(), authorizations);
            job.setInputFormatClass(AccumuloEdgeInputFormat.class);
        } else {
            throw new LumifyException("Unhandled element type: " + elementType);
        }
    }

    protected abstract void setupJobMapper(Job job);

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
