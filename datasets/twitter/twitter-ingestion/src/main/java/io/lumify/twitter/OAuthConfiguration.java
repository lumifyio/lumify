package io.lumify.twitter;

import static com.google.common.base.Preconditions.checkArgument;
import net.jcip.annotations.Immutable;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Strings;

/**
 * Utility class used to hold OAuth configuration parameters
 */
@Immutable
public final class OAuthConfiguration {
    private final String consumerKey;
    private final String consumerSecret;
    private final String token;
    private final String tokenSecret;


    /**
     *
     * @param cKey The associated consumer key, not null or empty
     * @param cSecret The associated consumer secret, not null or empty
     * @param tkn The associated token, not null or empty
     * @param tknSecret The associated token secret, not null or empty
     */
    public OAuthConfiguration(final String cKey, final String cSecret, final String tkn, final String tknSecret) {
        checkArgument(!Strings.isNullOrEmpty(cKey));
        checkArgument(!Strings.isNullOrEmpty(cSecret));
        checkArgument(!Strings.isNullOrEmpty(tkn));
        checkArgument(!Strings.isNullOrEmpty(tknSecret));

        consumerKey = cKey;
        consumerSecret = cSecret;
        token = tkn;
        tokenSecret = tknSecret;
    }

    public String getConsumerKey() {
        return consumerKey;
    }

    public String getConsumerSecret() {
        return consumerSecret;
    }

    public String getToken() {
        return token;
    }

    public String getTokenSecret() {
        return tokenSecret;
    }


    @Override
    public String toString() {
        final ToStringHelper helper = Objects.toStringHelper(this.getClass())
            .add("Consumer Key", consumerKey)
            .add("Consumer Secret", consumerSecret)
            .add("Token", token)
            .add("Token Secret", tokenSecret);

        return helper.toString();
    }
}
