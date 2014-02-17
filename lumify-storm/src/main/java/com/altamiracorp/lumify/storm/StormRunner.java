package com.altamiracorp.lumify.storm;

import backtype.storm.generated.StormTopology;
import backtype.storm.topology.TopologyBuilder;
import com.altamiracorp.lumify.core.model.workQueue.WorkQueueRepository;
import com.altamiracorp.lumify.storm.textHighlighting.TextHighlightingBolt;

public class StormRunner extends StormRunnerBase {
    private static final String TOPOLOGY_NAME = "lumify";
    private final String TEXT_HIGHLIGHT_SPOUT = "textHighlightSpout";
    private final String USER_TEXT_HIGHLIGHT_SPOUT = "userTextHighlightSpout";
    private final String TEXT_HIGHLIGHT_BOLT = "textHighlightBolt";

    public static void main(String[] args) throws Exception {
        int res = new StormRunner().run(args);
        if (res != 0) {
            System.exit(res);
        }
    }

    @Override
    protected String getTopologyName() {
        return TOPOLOGY_NAME;
    }

    public StormTopology createTopology(int parallelismHint) {
        TopologyBuilder builder = new TopologyBuilder();
        createArtifactHighlightingTopology(builder, parallelismHint);
        return builder.createTopology();
    }

    private void createArtifactHighlightingTopology(TopologyBuilder builder, int parallelismHint) {
        builder.setSpout(TEXT_HIGHLIGHT_SPOUT, createWorkQueueRepositorySpout(WorkQueueRepository.TEXT_HIGHLIGHT_QUEUE_NAME), 1)
                .setMaxTaskParallelism(1);
        builder.setSpout(USER_TEXT_HIGHLIGHT_SPOUT, createWorkQueueRepositorySpout(WorkQueueRepository.USER_TEXT_HIGHLIGHT_QUEUE_NAME), 1)
                .setMaxTaskParallelism(1);
        builder.setBolt(TEXT_HIGHLIGHT_BOLT, new TextHighlightingBolt(), parallelismHint)
                .shuffleGrouping(TEXT_HIGHLIGHT_SPOUT)
                .shuffleGrouping(USER_TEXT_HIGHLIGHT_SPOUT);
    }
}