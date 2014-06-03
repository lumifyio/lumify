package io.lumify.reindexmr;


import org.apache.hadoop.mapreduce.Mapper;
import org.securegraph.Authorizations;
import org.securegraph.GraphFactory;
import org.securegraph.Vertex;
import org.securegraph.accumulo.AccumuloAuthorizations;
import org.securegraph.accumulo.AccumuloGraph;
import org.securegraph.accumulo.mapreduce.SecureGraphMRUtils;
import org.securegraph.util.MapUtils;

import java.io.IOException;
import java.util.Map;

public class ReindexMRMapper extends Mapper<String, Vertex, Object, Vertex> {
    private AccumuloGraph graph;
    private Authorizations authorizations;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);
        Map configurationMap = SecureGraphMRUtils.toMap(context.getConfiguration());
        this.graph = (AccumuloGraph) new GraphFactory().createGraph(MapUtils.getAllWithPrefix(configurationMap, "graph"));
        this.authorizations = new AccumuloAuthorizations(context.getConfiguration().getStrings(SecureGraphMRUtils.CONFIG_AUTHORIZATIONS));
    }

    @Override
    protected void cleanup(Context context) throws IOException, InterruptedException {
        graph.shutdown();
        super.cleanup(context);
    }

    @Override
    protected void map(String rowKey, Vertex vertex, Context context) throws IOException, InterruptedException {
        graph.getSearchIndex().addElement(graph, vertex, authorizations);
    }
}
