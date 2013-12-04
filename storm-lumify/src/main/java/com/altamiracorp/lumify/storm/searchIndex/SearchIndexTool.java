package com.altamiracorp.lumify.storm.searchIndex;

import com.altamiracorp.lumify.core.cmdline.CommandLineBase;
import com.altamiracorp.lumify.core.model.graph.GraphRepository;
import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.model.workQueue.WorkQueueRepository;
import com.google.inject.Inject;
import org.apache.commons.cli.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SearchIndexTool extends CommandLineBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchIndexTool.class);
    private WorkQueueRepository workQueueRepository;
    private GraphRepository graphRepository;
    private int pageSize = 100;

    public static void main(String[] args) throws Exception {
        int res = new SearchIndexTool().run(args);
        if (res != 0) {
            System.exit(res);
        }
    }

    public SearchIndexTool() {
        initFramework = true;
    }

    @Override
    protected int run(CommandLine cmd) throws Exception {
        LOGGER.info("Adding all nodes to kafka");

        for (int offset = 0; ; offset += pageSize) {
            LOGGER.info("Adding vertices from offset " + offset);
            List<GraphVertex> graphVertices = graphRepository.searchAllVertices(offset, pageSize, getUser());
            if (graphVertices.size() == 0) {
                break;
            }
            for (GraphVertex graphVertex : graphVertices) {
                workQueueRepository.pushSearchIndex(graphVertex.getId());
            }
        }

        return 0;
    }

    @Inject
    public void setWorkQueueRepository(WorkQueueRepository workQueueRepository) {
        this.workQueueRepository = workQueueRepository;
    }

    @Inject
    public void setGraphRepository(GraphRepository graphRepository) {
        this.graphRepository = graphRepository;
    }
}
