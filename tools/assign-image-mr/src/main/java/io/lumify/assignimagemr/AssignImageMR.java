package io.lumify.assignimagemr;

import com.google.inject.Inject;
import io.lumify.core.mapreduce.LumifyMRBase;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.util.ToolRunner;
import org.securegraph.Graph;
import org.securegraph.accumulo.AccumuloGraph;
import org.securegraph.accumulo.mapreduce.AccumuloElementOutputFormat;
import org.securegraph.accumulo.mapreduce.AccumuloVertexInputFormat;

public class AssignImageMR extends LumifyMRBase {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(AssignImageMR.class);
    private AccumuloGraph graph;

    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new AssignImageMR(), args);
        System.exit(res);
    }

    @Override
    protected void setupJob(Job job) throws Exception {
        String[] authorizations = new String[]{
                LumifyVisibility.SUPER_USER_VISIBILITY_STRING
        };

        job.getConfiguration().setBoolean("mapred.map.tasks.speculative.execution", false);
        job.getConfiguration().setBoolean("mapred.reduce.tasks.speculative.execution", false);

        job.setJarByClass(AssignImageMR.class);
        job.setMapperClass(AssignImageMRMapper.class);
        job.setOutputFormatClass(AccumuloElementOutputFormat.class);
        job.setNumReduceTasks(0);

        job.setInputFormatClass(AccumuloVertexInputFormat.class);
        AccumuloVertexInputFormat.setInputInfo(job, graph, getInstanceName(), getZooKeepers(), getPrincipal(), getAuthorizationToken(), authorizations);
    }

    @Override
    protected void parseArgs(JobConf conf, String[] args) {
    }

    @Override
    protected String getJobName() {
        return "lumifyAssignImage";
    }

    @Inject
    public void setGraph(Graph graph) {
        this.graph = (AccumuloGraph) graph;
    }
}
