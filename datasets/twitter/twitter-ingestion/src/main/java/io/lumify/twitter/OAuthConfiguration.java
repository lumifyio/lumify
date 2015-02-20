package io.lumify.twitter;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Strings;

public final class OAuthConfiguration {
    private final String consumerKey;
    private final String consumerSecret;
    private final String token;
    private final String tokenSecret;


    public OAuthConfiguration(final String cKey, final String cSecret, final String tkn, final String tknSecret) {
        checkArgument(!Strings.isNullOrEmpty(cKey), "'consumerKey' config not set");
        checkArgument(!Strings.isNullOrEmpty(cSecret), "'consumerSecret' config not set");
        checkArgument(!Strings.isNullOrEmpty(tkn), "'token' config not set");
        checkArgument(!Strings.isNullOrEmpty(tknSecret), "'tokenSecret' config not set");

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
