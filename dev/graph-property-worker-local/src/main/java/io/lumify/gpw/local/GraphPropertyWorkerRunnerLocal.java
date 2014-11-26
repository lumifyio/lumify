package io.lumify.gpw.local;

import io.lumify.core.bootstrap.InjectHelper;
import io.lumify.core.cmdline.CommandLineBase;
import io.lumify.core.ingest.graphProperty.GraphPropertyRunner;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkerSpout;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkerTuple;
import org.apache.commons.cli.CommandLine;

public class GraphPropertyWorkerRunnerLocal extends CommandLineBase {
    public static void main(String[] args) throws Exception {
        int res = new GraphPropertyWorkerRunnerLocal().run(args);
        if (res != 0) {
            System.exit(res);
        }
    }

    @Override
    protected int run(CommandLine cmd) throws Exception {
        GraphPropertyWorkerSpout graphPropertyWorkerSpout = prepareGraphPropertyWorkerSpout();
        GraphPropertyRunner graphPropertyRunner = prepareGraphPropertyRunner();

        while (true) {
            GraphPropertyWorkerTuple tuple = graphPropertyWorkerSpout.nextTuple();
            if (tuple == null) {
                Thread.sleep(100);
                continue;
            }
            try {
                graphPropertyRunner.process(tuple.getJson());
                graphPropertyWorkerSpout.ack(tuple.getMessageId());
            } catch (Throwable ex) {
                LOGGER.error("Could not process tuple: %s", tuple, ex);
                graphPropertyWorkerSpout.fail(tuple.getMessageId());
            }
        }
    }

    private GraphPropertyWorkerSpout prepareGraphPropertyWorkerSpout() {
        GraphPropertyWorkerSpout spout = getWorkQueueRepository().createGraphPropertyWorkerSpout();
        spout.open();
        return spout;
    }

    private GraphPropertyRunner prepareGraphPropertyRunner() {
        GraphPropertyRunner graphPropertyRunner = InjectHelper.getInstance(GraphPropertyRunner.class);
        graphPropertyRunner.prepare(getUser());
        return graphPropertyRunner;
    }
}
