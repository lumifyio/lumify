package twitter4j;

import static com.google.common.base.Preconditions.checkNotNull;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

/**
 * Provides a workaround "hack" to generate a {@link Status} instance for retweets since Twitter4J doesn't store
 * retweeted status data in its underlying JSON representation
 */
public class RetweetStatusFactory {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(RetweetStatusFactory.class);

    /**
     * Provides a workaround "hack" to generate a {@link Status} instance for retweets since Twitter4J doesn't store
     * retweeted status data in its underlying JSON representation. Use with caution.
     * @param tweetStatus The tweet status that contains retweeted status data, not null
     * @return A Status instance for retweeted data, null if an error occurred
     */
    public static Status createRetweetedStatus(final Status tweetStatus) {
        checkNotNull(tweetStatus);

        // Create a config object that indicates that the data will be stored as JSON
        final Configuration config = new ConfigurationBuilder().setJSONStoreEnabled(true).build();
        try {
            // Convert the provided tweet status to JSON
            final JSONObject statusJsonObj = new JSONObject(TwitterObjectFactory.getRawJSON(tweetStatus));

            // Create a new status instance containing the retweeted status of the original tweet status; the retweet will be stored internally by Twitter4J
            return new StatusJSONImpl(statusJsonObj.getJSONObject("retweeted_status"), config);
        } catch (TwitterException | JSONException e) {
            LOGGER.error("Error occurred when creating modified retweeted status instance");
        }

        return null;
    }
}
