package io.lumify.reindexmr;


import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.hadoop.mapreduce.Mapper;
import org.securegraph.Authorizations;
import org.securegraph.Element;
import org.securegraph.GraphFactory;
import org.securegraph.Vertex;
import org.securegraph.accumulo.AccumuloAuthorizations;
import org.securegraph.accumulo.AccumuloGraph;
import org.securegraph.accumulo.mapreduce.SecureGraphMRUtils;
import org.securegraph.util.MapUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReindexMRMapper extends Mapper<String, Vertex, Object, Vertex> {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ReindexMRMapper.class);
    private static final int BATCH_SIZE = 100;
    private AccumuloGraph graph;
    private Authorizations authorizations;
    private List<Element> elementCache = new ArrayList<Element>(BATCH_SIZE);

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);
        LOGGER.info("setup: " + toString(context.getInputSplit().getLocations()));
        Map configurationMap = SecureGraphMRUtils.toMap(context.getConfiguration());
        this.graph = (AccumuloGraph) new GraphFactory().createGraph(MapUtils.getAllWithPrefix(configurationMap, "graph"));
        this.authorizations = new AccumuloAuthorizations(context.getConfiguration().getStrings(SecureGraphMRUtils.CONFIG_AUTHORIZATIONS));
    }

    private String toString(String[] locations) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < locations.length; i++) {
            if (i > 0) {
                result.append(", ");
            }
            result.append(locations[i]);
        }
        return result.toString();
    }

    @Override
    protected void cleanup(Context context) throws IOException, InterruptedException {
        writeCache();
        LOGGER.info("cleanup");
        graph.shutdown();
        super.cleanup(context);
    }

    @Override
    protected void map(String rowKey, Vertex vertex, Context context) throws IOException, InterruptedException {
        context.setStatus("Vertex Id: " + vertex.getId());
        elementCache.add(vertex);
        if (elementCache.size() >= BATCH_SIZE) {
            writeCache();
        }
    }

    private void writeCache() {
        if (elementCache.size() == 0) {
            return;
        }

        try {
            graph.getSearchIndex().addElements(graph, elementCache, authorizations);
        } catch (Throwable ex) {
            LOGGER.error("Could not add elements", ex);
        }
        elementCache.clear();
    }
}
