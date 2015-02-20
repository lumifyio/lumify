package io.lumify.twitter;

import io.lumify.core.bootstrap.InjectHelper;
import io.lumify.core.cmdline.CommandLineBase;
import io.lumify.core.config.Configuration;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;

import org.apache.commons.cli.CommandLine;

import twitter4j.TwitterException;

public final class TwitterDataIngestRunner extends CommandLineBase {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(TwitterDataIngestRunner.class);

    private static final String CONSUMER_KEY = "twitter.consumerKey";
    private static final String CONSUMER_SECRET = "twitter.consumerSecret";
    private static final String TOKEN = "twitter.token";
    private static final String TOKEN_SECRET = "twitter.tokenSecret";
    private static final int SUCCESSFUL_EXIT = 0;
    private static final int FAILURE_EXIT = 0;


    public static void main(String[] args) throws Exception {
        final CommandLineBase cliRunner = new TwitterDataIngestRunner();
        final int result = cliRunner.run(args);

        if( result != SUCCESSFUL_EXIT ) {
            System.exit(result);
        }
    }

    @Override
    protected int run(CommandLine cmd) {
        int exitCode = SUCCESSFUL_EXIT;

        final Configuration appConfig = getConfiguration();
        final OAuthConfiguration authConfig = new OAuthConfiguration(appConfig.get(CONSUMER_KEY, null),
                appConfig.get(CONSUMER_SECRET, null),
                appConfig.get(TOKEN, null),
                appConfig.get(TOKEN_SECRET, null));

        final TweetExtractor extractor = InjectHelper.getInstance(TweetExtractor.class);

        LOGGER.info("Running data ingestion");
        final long startTime = System.currentTimeMillis();

        try {
            extractor.doWork(authConfig);
            LOGGER.info("Data ingestion completed in %d ms", System.currentTimeMillis() - startTime);
        } catch (final TwitterException e) {
            LOGGER.error("Error occurred during data ingestion", e);
            exitCode = FAILURE_EXIT;
        }

        return exitCode;
    }
}
