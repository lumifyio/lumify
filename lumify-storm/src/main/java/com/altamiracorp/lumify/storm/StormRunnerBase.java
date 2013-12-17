package com.altamiracorp.lumify.storm;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.generated.StormTopology;
import backtype.storm.utils.Utils;
import com.altamiracorp.lumify.core.cmdline.CommandLineBase;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

public abstract class StormRunnerBase extends CommandLineBase {
    private static final String CMD_OPT_LOCAL = "local";
    private static final String CMD_OPT_TASK_COUNT = "taskcount";
    private boolean local;

    public StormRunnerBase() {
        initFramework = true;
    }

    @Override
    protected Options getOptions() {
        Options opts = super.getOptions();

        opts.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_LOCAL)
                        .withDescription("Run local")
                        .create()
        );

        opts.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_TASK_COUNT)
                        .withDescription("Max number of storm tasks")
                        .hasArg()
                        .withArgName("count")
                        .create()
        );

        return opts;
    }

    @Override
    protected int run(CommandLine cmd) throws Exception {
        local = cmd.hasOption(CMD_OPT_LOCAL);

        Config conf = createConfig(cmd);

        beforeCreateTopology(cmd, conf);

        StormTopology topology = createTopology();
        LOGGER.info("Created topology layout: " + topology);
        LOGGER.info(String.format("Submitting topology '%s'", getTopologyName()));

        if (local) {
            LocalCluster cluster = new LocalCluster();
            cluster.submitTopology(getTopologyName(), conf, topology);

            while (!willExit()) {
                Utils.sleep(100);
            }

            cluster.killTopology(getTopologyName());
            cluster.shutdown();
        } else {
            StormSubmitter.submitTopology(getTopologyName(), conf, topology);
        }

        return 0;
    }

    protected void beforeCreateTopology(CommandLine cmd, Config conf) throws Exception {

    }

    protected Config createConfig(CommandLine cmd) {
        Config conf = new Config();
        conf.put("topology.kryo.factory", "com.altamiracorp.lumify.storm.DefaultKryoFactory");
        for (String key : getConfiguration().getKeys()) {
            conf.put(key, getConfiguration().get(key));
        }
        conf.put(Config.TOPOLOGY_MESSAGE_TIMEOUT_SECS, 10000);
        conf.put(Config.TOPOLOGY_MAX_SPOUT_PENDING, 100);
        conf.put(Config.WORKER_CHILDOPTS, " -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port=1%ID% ");
        if (cmd.hasOption(CMD_OPT_TASK_COUNT)) {
            conf.put(Config.TOPOLOGY_TASKS, Integer.parseInt(cmd.getOptionValue(CMD_OPT_TASK_COUNT)));
        }
        conf.setDebug(false);
        conf.setNumWorkers(2);
        return conf;
    }

    protected abstract String getTopologyName();

    protected abstract StormTopology createTopology();

    protected boolean isLocal() {
        return local;
    }
}
