package io.lumify.gpw.yarn;

import io.lumify.core.cmdline.CommandLineBase;
import io.lumify.core.util.LumifyLogger;
import io.lumify.gpw.MediaPropertyConfiguration;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
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

        TwillController controller = weaveRunner.prepare(new GraphPropertyWorkerRunnable())
                .withDependencies(LumifyLogger.class) // core
                .withDependencies(MediaPropertyConfiguration.class) // graph-property-worker-base
                .addLogHandler(new PrinterLogHandler(new PrintWriter(System.out, true)))
                .start();

        Services.getCompletionFuture(controller).get();
        return 0;
    }
}
