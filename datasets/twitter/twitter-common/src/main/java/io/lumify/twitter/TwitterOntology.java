package io.lumify.twitter;

import io.lumify.core.model.properties.types.StringLumifyProperty;

public class TwitterOntology {
    public static final String EDGE_LABEL_TWEETED = "http://lumify.io/twitter#tweeted";
    public static final String EDGE_LABEL_MENTIONED = "http://lumify.io/twitter#mentioned";
    public static final String EDGE_LABEL_REFERENCED_URL = "http://lumify.io/twitter#refUrl";
    public static final String EDGE_LABEL_TAGGED = "http://lumify.io/twitter#tagged";
    public static final String EDGE_LABEL_RETWEET = "http://lumify.io/twitter#retweet";

    public static final String CONCEPT_TYPE_USER = "http://lumify.io/twitter#user";
    public static final String CONCEPT_TYPE_TWEET = "http://lumify.io/twitter#tweet";
    public static final String CONCEPT_TYPE_HASHTAG = "http://lumify.io/twitter#hashtag";
    public static final String CONCEPT_TYPE_URL = "http://lumify.io/twitter#url";
    public static final String CONCEPT_TYPE_PROFILE_IMAGE = "http://lumify.io/twitter#profileImage";

    public static final StringLumifyProperty PROFILE_IMAGE_URL = new StringLumifyProperty("http://lumify.io/twitter#profileImageUrl");
    public static final StringLumifyProperty SCREEN_NAME = new StringLumifyProperty("http://lumify.io/twitter#screenName");
}
