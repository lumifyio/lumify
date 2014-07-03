package io.lumify.twitter;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import backtype.storm.utils.Utils;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class Twitter4jSpout extends BaseRichSpout {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(Twitter4jSpout.class);
    public static final int DELAY_TIME = 15 * 60 * 1000;
    private SpoutOutputCollector collector;
    private static final String CONSUMER_KEY = "twitter.consumerKey";
    private static final String CONSUMER_SECRET = "twitter.consumerSecret";
    private static final String TOKEN = "twitter.token";
    private static final String TOKEN_SECRET = "twitter.tokenSecret";
    private Twitter twitter;

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declare(new Fields(TweetFileSpout.JSON_OUTPUT_FIELD));
    }

    @Override
    public void open(Map stormConf, TopologyContext context, SpoutOutputCollector collector) {
        String consumerKey = (String) stormConf.get(CONSUMER_KEY);
        String consumerSecret = (String) stormConf.get(CONSUMER_SECRET);
        String token = (String) stormConf.get(TOKEN);
        String tokenSecret = (String) stormConf.get(TOKEN_SECRET);

        checkNotNull(consumerKey, "'consumerKey' config not set");
        checkNotNull(consumerSecret, "'consumerSecret' config not set");
        checkNotNull(token, "'token' config not set");
        checkNotNull(tokenSecret, "'tokenSecret' config not set");

        LOGGER.info("Configuring environment for spout: %s-%d", context.getThisComponentId(), context.getThisTaskId());
        this.collector = collector;

        connect(
                consumerKey,
                consumerSecret,
                token,
                tokenSecret);
    }

    private void connect(String consumerKey, String consumerSecret, String token, String tokenSecret) {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb
                .setJSONStoreEnabled(true)
                .setDebugEnabled(true)
                .setOAuthConsumerKey(consumerKey)
                .setOAuthConsumerSecret(consumerSecret)
                .setOAuthAccessToken(token)
                .setOAuthAccessTokenSecret(tokenSecret);
        TwitterFactory tf = new TwitterFactory(cb.build());
        twitter = tf.getInstance();
    }

    @Override
    public void nextTuple() {
        try {
            ResponseList<Status> timeline = twitter.getHomeTimeline();
            for (Status status : timeline) {
                long tweetId = status.getId();
                String raw = TwitterObjectFactory.getRawJSON(status);
                LOGGER.debug("received tweet to process: %d", tweetId);
                collector.emit(new Values(raw), tweetId);
            }
            LOGGER.info("sleeping for %dms", DELAY_TIME);
            Utils.sleep(DELAY_TIME); // Sleep 15 minutes and then pool again
        } catch (TwitterException e) {
            collector.reportError(e);
        }
    }
}