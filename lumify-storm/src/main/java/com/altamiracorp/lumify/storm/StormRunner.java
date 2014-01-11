package com.altamiracorp.lumify.storm;

import backtype.storm.generated.StormTopology;
import backtype.storm.topology.TopologyBuilder;
import com.altamiracorp.lumify.core.model.workQueue.WorkQueueRepository;
import com.altamiracorp.lumify.storm.textHighlighting.ArtifactHighlightingBolt;

public class StormRunner extends StormRunnerBase {
    private static final String TOPOLOGY_NAME = "lumify";
    private final String ARTIFACT_HIGHLIGHT_SPOUT = "artifactHighlightSpout";
    private final String USER_ARTIFACT_HIGHLIGHT_SPOUT = "userArtifactHighlightSpout";
    private final String ARTIFACT_HIGHLIGHT_BOLT = "artifactHighlightBolt";

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
        builder.setSpout(ARTIFACT_HIGHLIGHT_SPOUT, createWorkQueueRepositorySpout(WorkQueueRepository.ARTIFACT_HIGHLIGHT_QUEUE_NAME), 1)
                .setMaxTaskParallelism(1);
        builder.setSpout(USER_ARTIFACT_HIGHLIGHT_SPOUT, createWorkQueueRepositorySpout(WorkQueueRepository.USER_ARTIFACT_HIGHLIGHT_QUEUE_NAME), 1)
                .setMaxTaskParallelism(1);
        builder.setBolt(ARTIFACT_HIGHLIGHT_BOLT, new ArtifactHighlightingBolt(), parallelismHint)
                .shuffleGrouping(ARTIFACT_HIGHLIGHT_SPOUT)
                .shuffleGrouping(USER_ARTIFACT_HIGHLIGHT_SPOUT);
    }
}