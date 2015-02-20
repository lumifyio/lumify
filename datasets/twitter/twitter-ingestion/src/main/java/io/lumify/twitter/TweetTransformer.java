package io.lumify.twitter;

import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.termMention.TermMentionBuilder;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workQueue.WorkQueueRepository;
import io.lumify.core.security.VisibilityTranslator;
import io.lumify.web.clientapi.model.VisibilityJson;

import java.io.ByteArrayInputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;
import org.securegraph.Authorizations;
import org.securegraph.Edge;
import org.securegraph.Graph;
import org.securegraph.Metadata;
import org.securegraph.Vertex;
import org.securegraph.VertexBuilder;
import org.securegraph.Visibility;
import org.securegraph.property.StreamingPropertyValue;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;

public final class TweetTransformer {

    private static final String MULTI_VALUE_KEY = TweetTransformer.class.getName();
    private static final String SOURCE_NAME = "twitter.com";
    private static final String PROCESS_TWITTER_INGEST = "twitter-ingest";

    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy");

    private final Graph graph;
    private final WorkQueueRepository workQueueRepository;
    private final UserRepository userRepository;
    private final VisibilityTranslator visibilityTranslator;
    private final Authorizations authorizations;

    private final Cache<String, Vertex> userVertexCache = CacheBuilder.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES).build();
    private final Cache<String, Vertex> urlVertexCache = CacheBuilder.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES).build();
    private final Cache<String, Vertex> hashtagVertexCache = CacheBuilder.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES).build();

    @Inject
    public TweetTransformer(final Graph graph, final WorkQueueRepository workQueueRepo,
                               final UserRepository userRepo, final  VisibilityTranslator translator) {
        this.graph = graph;
        workQueueRepository = workQueueRepo;
        userRepository = userRepo;
        visibilityTranslator = translator;

        authorizations = userRepository.getAuthorizations(userRepository.getSystemUser());
    }


    public Vertex createTweetVertex(String jsonString) {

        final JSONObject json = new JSONObject(jsonString);
        Vertex userVertex = getUserVertex(json.getJSONObject("user"));
        String vertexId = "TWEET_" + json.getLong("id");
        Visibility visibility = new Visibility("");
        VertexBuilder v = graph.prepareVertex(vertexId, visibility);


        LumifyProperties.CONCEPT_TYPE.addPropertyValue(v, MULTI_VALUE_KEY, TwitterOntology.CONCEPT_TYPE_TWEET, visibility);
        LumifyProperties.SOURCE.addPropertyValue(v, MULTI_VALUE_KEY, SOURCE_NAME, visibility);
        StreamingPropertyValue rawValue = new StreamingPropertyValue(new ByteArrayInputStream(jsonString.getBytes()), byte[].class);
        rawValue.searchIndex(false);

        LumifyProperties.RAW.addPropertyValue(v, MULTI_VALUE_KEY, rawValue, visibility);

        String text = json.getString("text");
        text = text.replaceAll("&lt;", "<").replaceAll("&gt;", ">").replaceAll("&amp;", "&");
        StreamingPropertyValue textValue = new StreamingPropertyValue(new ByteArrayInputStream(text.getBytes()), String.class);

        final Metadata textMetadata = new Metadata();
        textMetadata.add(LumifyProperties.META_DATA_TEXT_DESCRIPTION, "Tweet Text", visibilityTranslator.getDefaultVisibility());
        LumifyProperties.TEXT.addPropertyValue(v, MULTI_VALUE_KEY, textValue, textMetadata, visibility);

        String title = json.getJSONObject("user").getString("name") + ": " + text;
        LumifyProperties.TITLE.addPropertyValue(v, MULTI_VALUE_KEY, title, visibility);

        Date publishedDate = parseDate(json.getString("created_at"));
        if( publishedDate != null ) {
            LumifyProperties.PUBLISHED_DATE.addPropertyValue(v, MULTI_VALUE_KEY, publishedDate, visibility);
        }

        VisibilityJson visibilityJson = new VisibilityJson();
        LumifyProperties.VISIBILITY_JSON.setProperty(v, visibilityJson, visibility);


        Vertex tweetVertex = v.save(authorizations);
        graph.flush();

        workQueueRepository.pushGraphPropertyQueue(tweetVertex, LumifyProperties.RAW.getProperty(tweetVertex));
        workQueueRepository.pushGraphPropertyQueue(tweetVertex, LumifyProperties.TEXT.getProperty(tweetVertex));

        createTweetedEdge(userVertex, tweetVertex);
        processEntities(tweetVertex, json);
        processRetweetStatus(tweetVertex, json);
        return tweetVertex;
    }

    private Vertex getUserVertex(JSONObject userJson) {
        String vertexId = "TWITTER_USER_" + userJson.getLong("id");
        Vertex userVertex = userVertexCache.getIfPresent(vertexId);
        if( userVertex != null ) {
            return userVertex;
        }

        userVertex = graph.getVertex(vertexId, authorizations);
        if( userVertex == null ) {
            Visibility visibility = new Visibility("");
            VertexBuilder v = graph.prepareVertex(vertexId, visibility);

            LumifyProperties.CONCEPT_TYPE.addPropertyValue(v, MULTI_VALUE_KEY, TwitterOntology.CONCEPT_TYPE_USER, visibility);
            LumifyProperties.SOURCE.addPropertyValue(v, MULTI_VALUE_KEY, SOURCE_NAME, visibility);
            LumifyProperties.TITLE.addPropertyValue(v, MULTI_VALUE_KEY, userJson.getString("name"), visibility);

            TwitterOntology.SCREEN_NAME.addPropertyValue(v, MULTI_VALUE_KEY, userJson.getString("screen_name"), visibility);

            String profileImageUrl = userJson.optString("profile_image_url");
            if( profileImageUrl != null && profileImageUrl.length() > 0 ) {
                TwitterOntology.PROFILE_IMAGE_URL.addPropertyValue(v, MULTI_VALUE_KEY, profileImageUrl, visibility);
            }

            userVertex = v.save(authorizations);
            graph.flush();


            workQueueRepository.pushGraphPropertyQueue(userVertex, LumifyProperties.TITLE.getProperty(userVertex));

            if( profileImageUrl != null && profileImageUrl.length() > 0 ) {
                workQueueRepository.pushGraphPropertyQueue(userVertex, TwitterOntology.PROFILE_IMAGE_URL.getProperty(userVertex));
            }

            workQueueRepository.pushGraphPropertyQueue(userVertex, TwitterOntology.SCREEN_NAME.getProperty(userVertex));
        }

        userVertexCache.put(vertexId, userVertex);

        return userVertex;
    }



    private Date parseDate(String dateString) {
        try {
            if( dateString == null || dateString.length() == 0 ) {
                return null;
            }
            return DATE_FORMAT.parse(dateString);
        } catch (ParseException e) {
            throw new LumifyException("Could not parse date: " + dateString, e);
        }
    }

    private void createTweetedEdge(Vertex userVertex, Vertex tweetVertex) {
        Visibility visibility = new Visibility("");
        String tweetedEdgeId = userVertex.getId() + "_TWEETED_" + tweetVertex.getId();

        graph.addEdge(tweetedEdgeId, userVertex, tweetVertex, TwitterOntology.EDGE_LABEL_TWEETED, visibility, authorizations);
        graph.flush();
    }


    private void processRetweetStatus(Vertex tweetVertex, JSONObject json) {
        JSONObject retweetedStatus = json.optJSONObject("retweeted_status");
        if( retweetedStatus == null ) {
            return;
        }
        Vertex retweetedTweet = createTweetVertex(retweetedStatus.toString());
        Visibility visibility = new Visibility("");
        String retweetEdgeId = tweetVertex.getId() + "_RETWEET_" + retweetedTweet.getId();
        graph.addEdge(retweetEdgeId, retweetedTweet, tweetVertex, TwitterOntology.EDGE_LABEL_RETWEET, visibility, authorizations);
        graph.flush();
    }

    private void processEntities(Vertex tweetVertex, JSONObject json) {
        JSONObject entitiesJson = json.optJSONObject("entities");
        if( entitiesJson == null ) {
            return;
        }
        JSONArray hashtagsJson = entitiesJson.optJSONArray("hashtags");
        if( hashtagsJson != null ) {
            processHashtags(tweetVertex, hashtagsJson);
        }
        JSONArray urlsJson = entitiesJson.optJSONArray("urls");
        if( urlsJson != null ) {
            processUrls(tweetVertex, urlsJson);
        }
        JSONArray userMentionsJson = entitiesJson.optJSONArray("user_mentions");
        if( userMentionsJson != null ) {
            processUserMentions(tweetVertex, userMentionsJson);
        }
    }


    private void processUrls(Vertex tweetVertex, JSONArray urlsJson) {
        for (int i = 0; i < urlsJson.length(); i++) {
            processUrl(tweetVertex, urlsJson.getJSONObject(i));
        }
    }

    private void processUrl(Vertex tweetVertex, JSONObject urlJson) {
        Vertex urlVertex = getUrlVertex(urlJson);
        Edge edge = createReferencesUrlEdge(tweetVertex, urlVertex);
        JSONArray offsets = urlJson.getJSONArray("indices");
        createTermMention(tweetVertex, urlVertex, edge, TwitterOntology.CONCEPT_TYPE_HASHTAG, offsets);
    }



    private Vertex getUrlVertex(JSONObject urlJson) {
        String url = urlJson.optString("expanded_url");
        if( url == null ) {
            url = urlJson.getString("url");
        }
        String vertexId = "TWITTER_URL_" + url;
        Vertex urlVertex = urlVertexCache.getIfPresent(vertexId);
        if( urlVertex != null ) {
            return urlVertex;
        }
        urlVertex = graph.getVertex(vertexId, authorizations);
        if( urlVertex == null ) {
            Visibility visibility = new Visibility("");
            VertexBuilder v = graph.prepareVertex(vertexId, visibility);
            LumifyProperties.CONCEPT_TYPE.addPropertyValue(v, MULTI_VALUE_KEY, TwitterOntology.CONCEPT_TYPE_URL,
                    visibility);
            LumifyProperties.SOURCE.addPropertyValue(v, MULTI_VALUE_KEY, SOURCE_NAME, visibility);
            LumifyProperties.TITLE.addPropertyValue(v, MULTI_VALUE_KEY, url, visibility);
            urlVertex = v.save(authorizations);
            graph.flush();
            workQueueRepository.pushGraphPropertyQueue(urlVertex, LumifyProperties.TITLE.getProperty(urlVertex));
        }
        urlVertexCache.put(vertexId, urlVertex);
        return urlVertex;
    }

    private Edge createReferencesUrlEdge(Vertex tweetVertex, Vertex urlVertex) {
        Visibility visibility = new Visibility("");
        String mentionedEdgeId = tweetVertex.getId() + "_REFURL_" + urlVertex.getId();
        Edge edge = graph.addEdge(mentionedEdgeId, tweetVertex, urlVertex, TwitterOntology.EDGE_LABEL_REFERENCED_URL,
                visibility, authorizations);
        graph.flush();
        return edge;
    }



    private void processUserMentions(Vertex tweetVertex, JSONArray userMentionsJson) {
        for (int i = 0; i < userMentionsJson.length(); i++) {
            processUserMention(tweetVertex, userMentionsJson.getJSONObject(i));
        }
    }

    private void processUserMention(Vertex tweetVertex, JSONObject userMentionJson) {
        Vertex userVertex = getUserVertex(userMentionJson);
        Edge edge = createMentionedEdge(tweetVertex, userVertex);
        JSONArray offsets = userMentionJson.getJSONArray("indices");
        createTermMention(tweetVertex, userVertex, edge, TwitterOntology.CONCEPT_TYPE_HASHTAG, offsets);
    }


    private Edge createMentionedEdge(Vertex tweetVertex, Vertex userVertex) {
        Visibility visibility = new Visibility("");
        String mentionedEdgeId = tweetVertex.getId() + "_MENTIONED_" + userVertex.getId();
        Edge edge = graph.addEdge(mentionedEdgeId, tweetVertex, userVertex, TwitterOntology.EDGE_LABEL_MENTIONED, visibility, authorizations);
        graph.flush();

        return edge;
    }



    private void processHashtags(Vertex tweetVertex, JSONArray hashtagsJson) {
        for (int i = 0; i < hashtagsJson.length(); i++) {
            processHashtag(tweetVertex, hashtagsJson.getJSONObject(i));
        }
    }

    private void processHashtag(Vertex tweetVertex, JSONObject hashtagJson) {
        Vertex hashtagVertex = getHashtagVertex(hashtagJson);
        Edge edge = createTaggedEdge(tweetVertex, hashtagVertex);
        JSONArray offsets = hashtagJson.getJSONArray("indices");
        createTermMention(tweetVertex, hashtagVertex, edge, TwitterOntology.CONCEPT_TYPE_HASHTAG, offsets);
    }


    private Vertex getHashtagVertex(JSONObject hashtagJson) {
        String text = hashtagJson.optString("text");
        String vertexId = "TWITTER_HASHTAG_" + text.toLowerCase();
        Vertex hashtagVertex = hashtagVertexCache.getIfPresent(vertexId);
        if( hashtagVertex != null ) {
            return hashtagVertex;
        }
        hashtagVertex = graph.getVertex(vertexId, authorizations);
        if( hashtagVertex == null ) {
            Visibility visibility = new Visibility("");
            VertexBuilder v = graph.prepareVertex(vertexId, visibility);
            LumifyProperties.CONCEPT_TYPE.addPropertyValue(v, MULTI_VALUE_KEY, TwitterOntology.CONCEPT_TYPE_HASHTAG,
                    visibility);
            LumifyProperties.SOURCE.addPropertyValue(v, MULTI_VALUE_KEY, SOURCE_NAME, visibility);
            LumifyProperties.TITLE.addPropertyValue(v, MULTI_VALUE_KEY, text, visibility);
            hashtagVertex = v.save(authorizations);
            graph.flush();
            workQueueRepository
                    .pushGraphPropertyQueue(hashtagVertex, LumifyProperties.TITLE.getProperty(hashtagVertex));
        }
        hashtagVertexCache.put(vertexId, hashtagVertex);
        return hashtagVertex;
    }

    private Edge createTaggedEdge(Vertex tweetVertex, Vertex hashtagVertex) {
        Visibility visibility = new Visibility("");
        String mentionedEdgeId = tweetVertex.getId() + "_TAGGED_" + hashtagVertex.getId();
        Edge edge = graph.addEdge(mentionedEdgeId, tweetVertex, hashtagVertex, TwitterOntology.EDGE_LABEL_TAGGED,
                visibility, authorizations);
        graph.flush();
        return edge;
    }


    private void createTermMention(Vertex tweetVertex, Vertex vertex, Edge edge, String conceptUri, JSONArray offsets) {
        VisibilityJson visibilitySource = new VisibilityJson();
        long startOffset = offsets.getInt(0);
        long endOffset = offsets.getInt(1);
        String title = LumifyProperties.TITLE.getPropertyValue(vertex);

        new TermMentionBuilder()
            .sourceVertex(tweetVertex)
            .propertyKey(MULTI_VALUE_KEY)
            .start(startOffset)
            .end(endOffset)
            .title(title)
            .process(PROCESS_TWITTER_INGEST)
            .conceptIri(conceptUri)
            .visibilityJson(visibilitySource)
            .resolvedTo(vertex, edge)
            .save(graph, visibilityTranslator, authorizations);
    }
}
