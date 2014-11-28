package io.lumify.gpw.yarn;

import io.lumify.core.bootstrap.InjectHelper;
import io.lumify.core.cmdline.CommandLineBase;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.ingest.graphProperty.GraphPropertyRunner;
import io.lumify.core.user.User;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.twill.api.AbstractTwillRunnable;
import org.apache.twill.api.TwillController;
import org.apache.twill.api.TwillRunnerService;
import org.apache.twill.api.logging.PrinterLogHandler;
import org.apache.twill.common.Services;
import org.apache.twill.yarn.YarnTwillRunnerService;

import java.io.PrintWriter;

public class GraphPropertyWorkerRunnerYarn extends CommandLineBase {
    private static final String CMD_OPT_ZOOKEEPER = "zk";

    public static void main(String[] args) throws Exception {
        int res = new GraphPropertyWorkerRunnerYarn().run(args);
        if (res != 0) {
            System.exit(res);
        }
    }

    @Override
    protected Options getOptions() {
        Options options = super.getOptions();

        options.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_ZOOKEEPER)
                        .withDescription("ZooKeeper host:port [localhost:2181]")
                        .hasArg()
                        .isRequired()
                        .create("zk")
        );

        return options;
    }

    @Override
    protected int run(CommandLine cmd) throws Exception {
        String zkConnect = cmd.getOptionValue(CMD_OPT_ZOOKEEPER);
        TwillRunnerService weaveRunner = new YarnTwillRunnerService(new YarnConfiguration(), zkConnect);
        weaveRunner.startAndWait();

        TwillController controller = weaveRunner.prepare(new GraphPropertyWorkerRunnable(getUser()))
                .addLogHandler(new PrinterLogHandler(new PrintWriter(System.out, true)))
                .start();

        Services.getCompletionFuture(controller).get();
        return 0;
    }

    private static class GraphPropertyWorkerRunnable extends AbstractTwillRunnable {
        private final User user;

        public GraphPropertyWorkerRunnable(User user) {
            this.user = user;
        }

        @Override
        public void run() {
            try {
                GraphPropertyRunner graphPropertyRunner = prepareGraphPropertyRunner();
                graphPropertyRunner.run();
            } catch (Exception ex) {
                throw new LumifyException("GraphPropertyRunner failed", ex);
            }
        }

        private GraphPropertyRunner prepareGraphPropertyRunner() {
            GraphPropertyRunner graphPropertyRunner = InjectHelper.getInstance(GraphPropertyRunner.class);
            graphPropertyRunner.prepare(this.user);
            return graphPropertyRunner;
        }
    }
}
