package io.lumify.reindexmr;


import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.securegraph.Authorizations;
import org.securegraph.Element;
import org.securegraph.GraphFactory;
import org.securegraph.accumulo.AccumuloAuthorizations;
import org.securegraph.accumulo.AccumuloGraph;
import org.securegraph.accumulo.mapreduce.SecureGraphMRUtils;
import org.securegraph.util.MapUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReindexMRMapper extends Mapper<Text, Element, Object, Element> {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ReindexMRMapper.class);
    private static final int DEFAULT_BATCH_SIZE = 100;
    private AccumuloGraph graph;
    private Authorizations authorizations;
    private List<Element> elementCache;
    private int batchSize;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);
        LOGGER.info("setup: " + toString(context.getInputSplit().getLocations()));
        Map configurationMap = SecureGraphMRUtils.toMap(context.getConfiguration());
        batchSize = context.getConfiguration().getInt("reindex.batchsize", DEFAULT_BATCH_SIZE);
        elementCache = new ArrayList<Element>(batchSize);
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
    protected void map(Text rowKey, Element element, Context context) throws IOException, InterruptedException {
        try {
            safeMap(element, context);
        } catch (Throwable ex) {
            LOGGER.error("Failed to process element", ex);
        }
    }

    private void safeMap(Element element, Context context) {
        if (element == null) {
            return;
        }
        context.setStatus("Element Id: " + element.getId());
        elementCache.add(element);
        if (elementCache.size() >= batchSize) {
            context.setStatus("Submitting batch: " + elementCache.size());
            writeCache();
        }
        context.getCounter(ReindexCounters.ELEMENTS_PROCESSED).increment(1);
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
