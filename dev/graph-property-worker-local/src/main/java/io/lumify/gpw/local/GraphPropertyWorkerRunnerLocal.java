package io.lumify.gpw.local;

import io.lumify.core.bootstrap.InjectHelper;
import io.lumify.core.cmdline.CommandLineBase;
import io.lumify.core.ingest.graphProperty.GraphPropertyRunner;
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
        GraphPropertyRunner graphPropertyRunner = prepareGraphPropertyRunner();
        graphPropertyRunner.run();
        return 0;
    }

    private GraphPropertyRunner prepareGraphPropertyRunner() {
        GraphPropertyRunner graphPropertyRunner = InjectHelper.getInstance(GraphPropertyRunner.class);
        graphPropertyRunner.prepare(getUser());
        return graphPropertyRunner;
    }
}
