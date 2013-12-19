package com.altamiracorp.lumify.storm;

import backtype.storm.generated.StormTopology;
import backtype.storm.topology.TopologyBuilder;
import com.altamiracorp.lumify.core.model.workQueue.WorkQueueRepository;
import com.altamiracorp.lumify.storm.textHighlighting.ArtifactHighlightingBolt;

public class StormRunner extends StormRunnerBase {
    private static final String TOPOLOGY_NAME = "lumify";

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

    public StormTopology createTopology() {
        TopologyBuilder builder = new TopologyBuilder();
        createArtifactHighlightingTopology(builder);
        return builder.createTopology();
    }

    private void createArtifactHighlightingTopology(TopologyBuilder builder) {
        builder.setSpout("artifactHighlightSpout", new LumifyKafkaSpout(getConfiguration(), WorkQueueRepository.ARTIFACT_HIGHLIGHT_QUEUE_NAME), 1)
                .setMaxTaskParallelism(1);
        builder.setBolt("artifactHighlightBolt", new ArtifactHighlightingBolt(), 1)
                .shuffleGrouping("artifactHighlightSpout");
    }
}