package io.lumify.twitter;

import backtype.storm.Config;
import backtype.storm.generated.StormTopology;
import backtype.storm.topology.IRichSpout;
import backtype.storm.topology.TopologyBuilder;
import io.lumify.core.exception.LumifyException;
import io.lumify.storm.StormRunnerBase;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

public class StormRunner extends StormRunnerBase {
    private static final String TOPOLOGY_NAME = "lumify-twitter";
    private static final String OPT_FILE_NAME = "filename";
    private static final String OPT_STREAM = "stream";
    private static final String OPT_TWITTER4J = "twitter4j";
    public static final String TWITTER_INPUT_METHOD = "twitter.inputMethod";
    private String fileName;
    private boolean stream;
    private boolean twitter4j;

    public static void main(String[] args) throws Exception {
        int res = new StormRunner().run(args);
        if (res != 0) {
            System.exit(res);
        }
    }

    @Override
    protected Options getOptions() {
        Options opts = super.getOptions();

        opts.addOption(
                OptionBuilder
                        .withLongOpt(OPT_FILE_NAME)
                        .withDescription("Name of file.")
                        .hasArg()
                        .withArgName("fileName")
                        .create("f")
        );

        opts.addOption(
                OptionBuilder
                        .withLongOpt(OPT_STREAM)
                        .withDescription("Stream the twitter data using HoseBirdClient.")
                        .create()
        );

        opts.addOption(
                OptionBuilder
                        .withLongOpt(OPT_TWITTER4J)
                        .withDescription("Get data using Twitter4j.")
                        .create()
        );

        return opts;
    }

    @Override
    protected String getTopologyName() {
        return TOPOLOGY_NAME;
    }

    @Override
    protected void beforeCreateTopology(CommandLine cmd, Config conf) throws Exception {
        super.beforeCreateTopology(cmd, conf);

        if (cmd.hasOption(OPT_FILE_NAME)) {
            fileName = cmd.getOptionValue(OPT_FILE_NAME);
            return;
        }

        if (cmd.hasOption(OPT_STREAM)) {
            stream = true;
            return;
        }

        if (cmd.hasOption(OPT_TWITTER4J)) {
            twitter4j = true;
            return;
        }

        String inputMethod = (String) conf.get(TWITTER_INPUT_METHOD);
        if (inputMethod != null) {
            if ("stream".equals(inputMethod)) {
                stream = true;
                return;
            }

            if ("twitter4j".equals(inputMethod)) {
                twitter4j = true;
                return;
            }
        }

        throw new LumifyException("You must specify one input method");
    }

    public StormTopology createTopology(int parallelismHint) {
        TopologyBuilder builder = new TopologyBuilder();
        createTweetProcessingTopology(builder, parallelismHint);
        return builder.createTopology();
    }

    private void createTweetProcessingTopology(TopologyBuilder builder, int parallelismHint) {
        IRichSpout spout;

        if (fileName != null) {
            spout = new TweetFileSpout(fileName);
        } else if (stream) {
            spout = new TwitterStreamSpout();
        } else if (twitter4j) {
            spout = new Twitter4jSpout();
        } else {
            throw new LumifyException("You must specify one input method");
        }

        String name = "lumify-twitter";
        builder.setSpout(name + "-spout", spout, 1)
                .setMaxTaskParallelism(1);
        builder.setBolt(name + "-bolt", new TweetProcessorBolt(), parallelismHint)
                .shuffleGrouping(name + "-spout");
    }
}