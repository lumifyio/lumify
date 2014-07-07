package io.lumify.twitter;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.securegraph.util.ConvertingIterable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.securegraph.util.IterableUtils.toList;

public class TwitterStreamSpout extends BaseRichSpout {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(TwitterStreamSpout.class);
    private SpoutOutputCollector collector;
    private final BlockingQueue<String> tweetsToProcess = new LinkedBlockingQueue<String>();

    private static final Pattern TWEET_ID_PATTERN = Pattern.compile("\"id_str\"\\s*:\\s*\"(\\d+)\"");

    private static final String TERMS = "twitter.terms";
    private static final String FOLLOWINGS = "twitter.followings";
    private static final String CONSUMER_KEY = "twitter.consumerKey";
    private static final String CONSUMER_SECRET = "twitter.consumerSecret";
    private static final String TOKEN = "twitter.token";
    private static final String TOKEN_SECRET = "twitter.tokenSecret";

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
        String terms = (String) stormConf.get(TERMS);
        String followings = (String) stormConf.get(FOLLOWINGS);

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
                tokenSecret,
                terms,
                followings);
    }

    private void connect(String consumerKey, String consumerSecret, String token, String tokenSecret, String terms, String followings) {
        Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);

        StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();

        List<String> termsList = getTermsListFromString(terms);
        if (termsList != null && termsList.size() > 0) {
            endpoint.trackTerms(termsList);
        }

        List<Long> followingsList = getFollowingsListFromString(followings);
        if (followingsList != null && followingsList.size() > 0) {
            endpoint.followings(followingsList);
        }

        Authentication hosebirdAuth = new OAuth1(
                consumerKey,
                consumerSecret,
                token,
                tokenSecret);

        ClientBuilder builder = new ClientBuilder()
                .name("twitter-spout")
                .hosts(hosebirdHosts)
                .endpoint(endpoint)
                .authentication(hosebirdAuth)
                .processor(new StringDelimitedProcessor(tweetsToProcess));

        Client hbc = builder.build();
        hbc.connect();
    }

    private List<String> getTermsListFromString(String terms) {
        if (terms == null || terms.length() == 0) {
            return null;
        }
        return Lists.newArrayList(terms.split(";"));
    }

    private List<Long> getFollowingsListFromString(final String followings) {
        if (followings == null || followings.length() == 0) {
            return null;
        }
        return toList(new ConvertingIterable<String, Long>(Lists.newArrayList(followings.split(";"))) {
            @Override
            protected Long convert(String s) {
                return Long.parseLong(s);
            }
        });
    }

    @Override
    public void nextTuple() {
        try {
            String tweet = tweetsToProcess.take();
            Matcher m = TWEET_ID_PATTERN.matcher(tweet);
            if (m.find()) {
                String tweetId = m.group(1);
                LOGGER.debug("received tweet to process: %s", tweetId);
                collector.emit(new Values(tweet), tweetId);
            } else {
                LOGGER.warn("Could not parse tweet id from: %s", tweet);
            }
        } catch (InterruptedException e) {
            collector.reportError(e);
        }
    }
}