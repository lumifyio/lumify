package io.lumify.gpw.yarn;

import com.google.common.util.concurrent.Service;
import io.lumify.core.cmdline.CommandLineBase;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.util.LumifyLogger;
import io.lumify.gpw.MediaPropertyConfiguration;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.twill.api.ResourceSpecification;
import org.apache.twill.api.TwillController;
import org.apache.twill.api.TwillRunnerService;
import org.apache.twill.api.logging.PrinterLogHandler;
import org.apache.twill.common.Services;
import org.apache.twill.yarn.YarnTwillRunnerService;

import java.io.PrintWriter;
import java.util.List;

import static org.securegraph.util.IterableUtils.toList;

public class GraphPropertyWorkerRunnerYarn extends CommandLineBase {
    private static final String CMD_OPT_ZOOKEEPER = "zk";
    private static final String CMD_OPT_INSTANCES = "instances";
    private static final String CMD_OPT_VIRTUAL_CORES = "virtual-cores";
    private static final String CMD_OPT_MEMORY = "memory";
    private static final String CMD_OPT_ALTER = "alter";
    private static final int DEFAULT_VIRTUAL_CORES = 1;
    private static final String DEFAULT_MEMORY = "512m";

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

        options.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_INSTANCES)
                        .withDescription("Number of instances to run")
                        .hasArg()
                        .isRequired()
                        .create()
        );

        options.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_VIRTUAL_CORES)
                        .withDescription("Number of virtual cores [default: " + DEFAULT_VIRTUAL_CORES + "]")
                        .hasArg()
                        .create()
        );

        options.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_MEMORY)
                        .withDescription("Amount of memory to use per instance [default: " + DEFAULT_MEMORY + "]")
                        .hasArg()
                        .create()
        );

        options.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_ALTER)
                        .withDescription("If alter is specified this will change an existing application")
                        .create()
        );

        return options;
    }

    @Override
    protected int run(CommandLine cmd) throws Exception {
        String zkConnect = cmd.getOptionValue(CMD_OPT_ZOOKEEPER);
        int instances = Integer.parseInt(cmd.getOptionValue(CMD_OPT_INSTANCES));
        int virtualCores = Integer.parseInt(cmd.getOptionValue(CMD_OPT_VIRTUAL_CORES, Integer.toString(DEFAULT_VIRTUAL_CORES)));
        Size memory = parseSize(cmd.getOptionValue(CMD_OPT_MEMORY, DEFAULT_MEMORY));
        TwillRunnerService weaveRunner = new YarnTwillRunnerService(new YarnConfiguration(), zkConnect);
        weaveRunner.startAndWait();

        if (cmd.hasOption(CMD_OPT_ALTER)) {
            List<TwillController> controllers = getControllers(weaveRunner);
            LOGGER.info("Found %d controllers", controllers.size());
            for (TwillController controller : controllers) {
                LOGGER.info("Changing instances to %d on controller with run id: %s", instances, controller.getRunId());
                Integer result = controller
                        .changeInstances(GraphPropertyWorkerApplication.RUNNABLE_NAME, instances)
                        .get();
                LOGGER.info("results: %s", result.toString());
            }
        } else {
            TwillController controller = weaveRunner.prepare(new GraphPropertyWorkerApplication(virtualCores, memory, instances))
                    .withDependencies(LumifyLogger.class) // core
                    .withDependencies(MediaPropertyConfiguration.class) // graph-property-worker-base
                    .addJVMOptions("-Djava.net.preferIPv4Stack=true")
                    .addLogHandler(new PrinterLogHandler(new PrintWriter(System.out, true)))
                    .start();

            Service.State result = Services.getCompletionFuture(controller).get();
            LOGGER.info("results: %s", result.toString());
        }
        return 0;
    }

    private List<TwillController> getControllers(TwillRunnerService weaveRunner) throws InterruptedException {
        int timeout = 0;
        List<TwillController> controllers = null;
        while (timeout < 10) {
            controllers = toList(weaveRunner.lookup(GraphPropertyWorkerApplication.NAME));
            if (controllers.size() > 0) {
                break;
            }
            LOGGER.debug("waiting for controller list...");
            Thread.sleep(1000);
            timeout++;
        }
        if (controllers.size() == 0) {
            throw new LumifyException("Could not find applications with name: " + GraphPropertyWorkerApplication.NAME);
        }
        return controllers;
    }

    private Size parseSize(String value) {
        ResourceSpecification.SizeUnit units;
        value = value.toLowerCase().trim();
        if (value.endsWith("m")) {
            units = ResourceSpecification.SizeUnit.MEGA;
        } else if (value.endsWith("g")) {
            units = ResourceSpecification.SizeUnit.GIGA;
        } else {
            throw new LumifyException("Invalid size suffix");
        }
        value = value.substring(0, value.length() - 1);
        int size = Integer.parseInt(value);
        return new Size(size, units);
    }

}
