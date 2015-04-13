package io.lumify.twitter.loaders;

import io.lumify.twitter.TweetTransformer;

import org.securegraph.Visibility;

public final class LoaderConstants {
    public static final Visibility EMPTY_VISIBILITY = new Visibility("");
    public static final String MULTI_VALUE_KEY = TweetTransformer.class.getName();
    public static final String SOURCE_NAME = "twitter.com";


    private LoaderConstants() {
        throw new AssertionError();
    }
}
