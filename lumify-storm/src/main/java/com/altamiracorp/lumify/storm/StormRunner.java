package com.altamiracorp.lumify.storm;

import backtype.storm.generated.StormTopology;
import backtype.storm.topology.TopologyBuilder;
import com.altamiracorp.lumify.core.model.workQueue.WorkQueueRepository;

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

    public StormTopology createTopology(int parallelismHint) {
        TopologyBuilder builder = new TopologyBuilder();
        createGraphPropertyTopology(builder, parallelismHint);
        return builder.createTopology();
    }

    private void createGraphPropertyTopology(TopologyBuilder builder, int parallelismHint) {
        String name = "graphProperty";
        builder.setSpout(name + "-spout", createWorkQueueRepositorySpout(WorkQueueRepository.GRAPH_PROPERTY_QUEUE_NAME), 1)
                .setMaxTaskParallelism(1);
        builder.setBolt(name + "-bolt", new GraphPropertyBolt(), parallelismHint)
                .shuffleGrouping(name + "-spout");
    }
}